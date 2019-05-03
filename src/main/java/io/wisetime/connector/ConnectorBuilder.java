/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.ApiClientMetricWrapper;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.fetch_client.FetchClient;
import io.wisetime.connector.fetch_client.FetchClientSpec;
import io.wisetime.connector.fetch_client.TimeGroupIdStore;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.webhook.WebhookApplication;
import io.wisetime.connector.webhook.WebhookFilter;
import io.wisetime.connector.webhook.WebhookServerRunner;

/**
 * Builder for {@link ConnectorController}. You have to provide an API key or custom {@link ApiClient} that will handle
 * authentication. For more information on how to obtain an API key, refer to:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 * <p>
 * You have to set a connector by calling {@link #withWiseTimeConnector(WiseTimeConnector)}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConnectorBuilder {

  private boolean persistentStorageOnly = false;
  private Boolean useFetchClient;
  private int fetchClientFetchLimit = 25;
  private WiseTimeConnector wiseTimeConnector;
  private ApiClient apiClient;
  private String apiKey;
  private MetricService metricService;

  private static final int DEFAULT_WEBHOOK_PORT = 8080;

  /**
   * Build {@link ConnectorController}. Make sure to set {@link WiseTimeConnector} and apiKey or apiClient before calling
   * this method.
   */
  public ConnectorController build() {
    final SQLiteHelper sqLiteHelper = new SQLiteHelper(persistentStorageOnly);

    if (apiClient == null) {
      if (StringUtils.isEmpty(apiKey)) {
        throw new IllegalArgumentException(
            "an apiKey must be supplied via constructor or environment parameter to use the default apiClient");
      }
      RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey);
      apiClient = new DefaultApiClient(requestExecutor);
    }

    if (metricService == null) {
      metricService = new MetricService();
    }

    // Wrap api client for metrics gathering
    apiClient = new ApiClientMetricWrapper(apiClient, metricService);

    if (wiseTimeConnector == null) {
      throw new IllegalArgumentException(
          String.format("an implementation of '%s' interface must be supplied", WiseTimeConnector.class.getSimpleName()));
    }

    ConnectorModule connectorModule = new ConnectorModule(
        apiClient,
        new FileStore(sqLiteHelper)
    );

    // if useFetchClient is null, no time posting mechanism was specified, therefore running in tag upload mode only
    TimePosterRunner timePosterRunner = new NoOpTimePosterRunner();
    if (useFetchClient != null) {
      if (useFetchClient) {
        TimeGroupIdStore timeGroupIdStore = new TimeGroupIdStore(sqLiteHelper);

        FetchClientSpec spec = new FetchClientSpec(apiClient, wiseTimeConnector, timeGroupIdStore, fetchClientFetchLimit);
        timePosterRunner = new FetchClient(spec);
      } else {
        int port = RuntimeConfig.getInt(ConnectorConfigKey.WEBHOOK_PORT).orElse(DEFAULT_WEBHOOK_PORT);

        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setDaemon(true);
        Server server = new Server(pool);

        WebAppContext webAppContext = createWebAppContext();

        initializeWebhookServer(port, server, webAppContext, metricService);

        timePosterRunner = new WebhookServerRunner(server);
      }
    }

    final TagRunner tagRunner = new TagRunner(wiseTimeConnector::performTagUpdate);
    final HealthCheck healthRunner = new HealthCheck(
        tagRunner::getLastSuccessfulRun,
        timePosterRunner::isHealthy,
        wiseTimeConnector::isConnectorHealthy
    );
    return new ConnectorRunner(
        timePosterRunner,
        wiseTimeConnector,
        connectorModule,
        tagRunner,
        healthRunner,
        metricService
    );
  }

  private void initializeWebhookServer(int port,
                                       Server server,
                                       WebAppContext webAppContext,
                                       MetricService metricService) {
    WebhookApplication sparkApp = new WebhookApplication(wiseTimeConnector, metricService);

    webAppContext.addFilter(
        new FilterHolder(new WebhookFilter(sparkApp)),
        "/*",
        null
    );

    HandlerCollection handlerCollection = new HandlerCollection(false);
    handlerCollection.addHandler(webAppContext);
    server.setHandler(handlerCollection);

    addCustomizers(port, server);
  }

  /**
   * Implementation of {@link WiseTimeConnector} is required to start runner.
   *
   * @param wiseTimeConnector see {@link WiseTimeConnector}
   */
  public ConnectorBuilder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector) {
    this.wiseTimeConnector = wiseTimeConnector;
    return this;
  }

  /**
   * More information about WiseTime API key: <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
   * <p>
   * You have to provide api key or custom implementation of {@link ApiClient} for successful authorization.
   *
   * @param apiKey see {@link #withApiClient(ApiClient)}
   */
  public ConnectorBuilder withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  /**
   * If persistent storage is required, this property forces the operator to set the DATA_DIR configuration parameter (via
   * system property or environment variable). The DATA_DIR directory will be used to persist data across connector
   * restarts.
   * <p>
   * Otherwise the connector will not force the operator to specify a DATA_DIR. It will still use the DATA_DIR path if one is
   * set. If none is provided, a subdirectory in the /tmp/ path will be used.
   * <p>
   * Default is false.
   *
   * @param persistentStorageOnly whether persistent storage is required for this connector.
   */
  public ConnectorBuilder requirePersistentStore(boolean persistentStorageOnly) {
    this.persistentStorageOnly = persistentStorageOnly;
    return this;
  }

  public String getApiKey() {
    return this.apiKey;
  }

  /**
   * Custom implementation of {@link ApiClient}. If set the API key property is ignored.
   *
   * @param apiClient see {@link #withApiKey(String)}
   */
  public ConnectorBuilder withApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
    return this;
  }

  /**
   * Method to signal that the fetch implementation for posted time groups should be used.
   */
  public ConnectorBuilder useFetchClient() {
    this.useFetchClient = true;
    return this;
  }

  /**
   * Sets the maximum amount of time groups to be fetched with each call.
   *
   * @param limit the maximum amount of time groups to be fetch. Valid values: 1-25
   */
  public ConnectorBuilder withFetchClientLimit(int limit) {
    if (!Boolean.TRUE.equals(useFetchClient)) {
      throw new IllegalStateException("Can only be used with the fetch client mechanism");
    }
    if (limit < 1 || limit > 25) {
      throw new IllegalArgumentException("The limit for time groups to be retrieved by the " +
          "fetch client must be between 1 and 25");
    }
    this.fetchClientFetchLimit = limit;
    return this;
  }

  /**
   * Method to signal that the webhook based implementation for posted time groups should be used.
   */
  public ConnectorBuilder useWebhook() {
    this.useFetchClient = false;
    return this;
  }

  @VisibleForTesting
  ConnectorBuilder withMetricService(MetricService metricService) {
    this.metricService = metricService;
    return this;
  }

  private WebAppContext createWebAppContext() {
    WebAppContext webAppContext = new WebAppContext();
    // load from standard class-loader instead of searching for WEB-INF/lib loader as we are running embedded
    webAppContext.setParentLoaderPriority(true);
    webAppContext.setContextPath("/");
    final String webAppFilePath = "/webapp/emptyService.txt";
    URL resource = this.getClass().getResource(webAppFilePath);
    if (resource == null) {
      throw new RuntimeException("should find file as base of webapp " + webAppFilePath);
    }
    webAppContext.setResourceBase(new File(resource.getFile()).getParent());

    return webAppContext;
  }

  private void addCustomizers(int port, Server server) {
    // Create HTTP Config & http connector
    HttpConfiguration httpConfig = createHttpConfiguration();
    HttpConnectionFactory connectionFactory = new HttpConnectionFactory(httpConfig);
    ServerConnector connector = new ServerConnector(server, connectionFactory);

    // Set the port on the connector, or the port in the Server constructor is overridden by the new connector,
    connector.setPort(port);
    // and lastly set connector to the server
    server.setConnectors(new ServerConnector[]{connector});
  }

  /**
   * Add connectors and customizers
   */
  private HttpConfiguration createHttpConfiguration() {
    HttpConfiguration httpConfig = new HttpConfiguration();
    // `http-forwarded` module as a customizer
    httpConfig.addCustomizer(new org.eclipse.jetty.server.ForwardedRequestCustomizer());
    return httpConfig;
  }
}
