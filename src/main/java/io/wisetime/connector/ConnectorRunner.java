/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.fetch_client.FetchClient;
import io.wisetime.connector.fetch_client.FetchClientSpec;
import io.wisetime.connector.fetch_client.TimeGroupIdStore;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.webhook.WebhookFilter;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.webhook.WebhookApplication;
import io.wisetime.connector.webhook.WebhookServerRunner;

/**
 * Main entry point of WiseTime connector. Instance of service is created by {@link ConnectorBuilder}:
 * <pre>
 *     ConnectorRunner runner = ConnectorRunner.createConnectorBuilder()
 *         .useWebhook()
 *         .withWiseTimeConnector(myConnector)
 *         .withApiKey(apiKey)
 *         .build();
 *     </pre>
 *
 * You can then start the connector by calling {@link #start()}. This is a blocking call, meaning that the current thread
 * will wait till the application dies.
 * <p>
 * Main extension point is {@link WiseTimeConnector}.
 * <p>
 * More information regarding the API key can be found here:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 *
 * @author thomas.haines@practiceinsight.io, pascal.filippi@staff.wisetime.com
 */
@SuppressWarnings("WeakerAccess")
public class ConnectorRunner {

  private final TimePosterRunner timePosterRunner;
  private final int port;
  private final WebAppContext webAppContext;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;

  @SuppressWarnings("ParameterNumber")
  private ConnectorRunner(TimePosterRunner timePosterRunner,
                          int port,
                          WebAppContext webAppContext,
                          WiseTimeConnector wiseTimeConnector,
                          ConnectorModule connectorModule) {
    this.timePosterRunner = timePosterRunner;
    this.port = port;
    this.webAppContext = webAppContext;
    this.wiseTimeConnector = wiseTimeConnector;
    this.connectorModule = connectorModule;
  }

  /**
   * Method to create new {@link ConnectorBuilder} instance. Automatically checks system properties for API_KEY.
   */
  public static ConnectorBuilder createConnectorBuilder() {
    ConnectorBuilder builder = new ConnectorBuilder();
    RuntimeConfig.getString(ConnectorConfigKey.API_KEY).ifPresent(builder::withApiKey);
    Optional<String> fetchClientId = RuntimeConfig.getString(ConnectorConfigKey.FETCH_CLIENT_ID);
    Optional<String> useWebhooks = RuntimeConfig.getString(ConnectorConfigKey.USE_WEBHOOKS);
    if (fetchClientId.isPresent() && useWebhooks.isPresent()) {
      throw new IllegalStateException("Only one of FETCH_CLIENT_ID or USE_WEBHOOKS environment variables should be set");
    }
    fetchClientId.ifPresent(builder::useFetchClient);
    RuntimeConfig.getString(ConnectorConfigKey.FETCH_CLIENT_LIMIT).map(Integer::parseInt)
        .ifPresent(builder::withFetchClientLimit);
    // Value is ignored, we just need a flag to enable webhooks
    useWebhooks.ifPresent(webhook -> builder.useWebhook());
    RuntimeConfig.getString(ConnectorConfigKey.JETTY_SERVER_SHUTDOWN_TOKEN).ifPresent(builder::withShutdownToken);
    return builder;
  }

  @SuppressWarnings("unused")
  public int getPort() {
    return port;
  }

  @SuppressWarnings("unused")
  public WebAppContext getWebAppContext() {
    return webAppContext;
  }

  /**
   * Start the runner. This will run scheduled tasks for tag updates and health checks.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   */
  public void start() throws Exception {
    initWiseTimeConnector();
    final TagRunner tagRunTask = new TagRunner(wiseTimeConnector::performTagUpdate);
    final HealthCheck healthRunner = new HealthCheck(
        tagRunTask::getLastSuccessfulRun,
        timePosterRunner::isHealthy,
        wiseTimeConnector::isConnectorHealthy
    );

    timePosterRunner.start();

    Timer healthCheckTimer = new Timer("health-check-timer");
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer");
    tagTimer.scheduleAtFixedRate(tagRunTask, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    while (!Thread.currentThread().isInterrupted() && timePosterRunner.isRunning()) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // ignore.
      }
    }
    timePosterRunner.stop();
    healthCheckTimer.cancel();
    healthCheckTimer.purge();
    tagTimer.cancel();
    tagTimer.purge();
    try {
      // leaves time for the threads to stop
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      // ignore.
    }
    throw new InterruptedException();
  }

  //visible for testing
  void initWiseTimeConnector() {
    wiseTimeConnector.init(connectorModule);
  }

  TimePosterRunner getTimePosterRunner() {
    return timePosterRunner;
  }

  /**
   * Builder for {@link ConnectorRunner}. You have to provide an API key or custom {@link ApiClient} that will handle
   * authentication. For more information on how to obtain an API key, refer to:
   * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
   * <p>
   * You have to set a connector by calling {@link #withWiseTimeConnector(WiseTimeConnector)}.
   */
  public static class ConnectorBuilder {

    private boolean persistentStorageOnly = false;
    private Boolean useFetchClient;
    private String fetchClientId;
    private int fetchClientFetchLimit = 25;
    private WiseTimeConnector wiseTimeConnector;
    private ApiClient apiClient;
    private String apiKey;
    private String shutdownToken;

    private static final int DEFAULT_WEBHOOK_PORT = 8080;

    /**
     * Build {@link ConnectorRunner}. Make sure to set {@link WiseTimeConnector} and apiKey or apiClient before calling this
     * method.
     */
    public ConnectorRunner build() {
      final SQLiteHelper sqLiteHelper = new SQLiteHelper(persistentStorageOnly);

      if (apiClient == null) {
        if (StringUtils.isEmpty(apiKey)) {
          throw new IllegalArgumentException(
              "an apiKey must be supplied via constructor or environment parameter to use the default apiClient");
        }
        RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey);
        apiClient = new DefaultApiClient(requestExecutor);
      }

      if (wiseTimeConnector == null) {
        throw new IllegalArgumentException(
            String.format("an implementation of '%s' interface must be supplied", WiseTimeConnector.class.getSimpleName()));
      }

      ConnectorModule connectorModule = new ConnectorModule(
          apiClient,
          new FileStore(sqLiteHelper)
      );

      int port = 0;
      WebAppContext webAppContext = null;
      // if useFetchClient is null, no time posting mechanism was specified, therefore running in tag upload mode only
      TimePosterRunner timePosterRunner = new NoOpTimePosterRunner();
      if (useFetchClient != null) {
        if (useFetchClient && StringUtils.isBlank(fetchClientId)) {
          throw new IllegalArgumentException("fetch client id can't be null or empty, " +
              "if connector is configured to use the fetch client");
        }

        if (useFetchClient) {
          TimeGroupIdStore timeGroupIdStore = new TimeGroupIdStore(sqLiteHelper);

          FetchClientSpec spec = new FetchClientSpec(apiClient, wiseTimeConnector, timeGroupIdStore,
              fetchClientId, fetchClientFetchLimit);
          timePosterRunner = new FetchClient(spec);
        } else {
          port = RuntimeConfig.getInt(ConnectorConfigKey.WEBHOOK_PORT).orElse(DEFAULT_WEBHOOK_PORT);

          Server server = new Server(port);

          webAppContext = createWebAppContext();

          initializeWebhookServer(port, server, webAppContext);

          timePosterRunner = new WebhookServerRunner(server, port);
        }
      }
      return new ConnectorRunner(timePosterRunner, port, webAppContext, wiseTimeConnector, connectorModule);
    }

    private void initializeWebhookServer(int port,
                                         Server server,
                                         WebAppContext webAppContext) {
      WebhookApplication sparkApp = new WebhookApplication(wiseTimeConnector);

      webAppContext.addFilter(
          new FilterHolder(new WebhookFilter(sparkApp)),
          "/*",
          null
      );

      HandlerCollection handlerCollection = new HandlerCollection(false);
      handlerCollection.addHandler(webAppContext);
      if (StringUtils.isNotBlank(shutdownToken)) {
        handlerCollection.addHandler(new ShutdownHandler(shutdownToken, false, true));
      }
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
     * A shutdown token is required to shutdown the runner externally.
     *
     * @param shutdownToken see {@link #withShutdownToken(String)}
     */
    public ConnectorBuilder withShutdownToken(String shutdownToken) {
      this.shutdownToken = shutdownToken;
      return this;
    }

    /**
     * If persistent storage is required, this property forces the operator to set the DATA_DIR configuration parameter (via
     * system property or environment variable). The DATA_DIR directory will be used to persist data across connector
     * restarts.
     * <p>
     * Otherwise the connector will not force the operator to specify a DATA_DIR. It will still use the DATA_DIR path if one
     * is set. If none is provided, a subdirectory in the /tmp/ path will be used.
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
     *
     * @param fetchClientId the id provided after registering a fetch client
     */
    public ConnectorBuilder useFetchClient(String fetchClientId) {
      if (useFetchClient != null) {
        throw new IllegalStateException("useFetchClient or useWebhook should only be called once");
      }
      this.fetchClientId = fetchClientId;
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
      if (useFetchClient != null) {
        throw new IllegalStateException("useFetchClient or useWebhook should only be called once");
      }
      this.useFetchClient = false;
      return this;
    }

    private WebAppContext createWebAppContext() {
      WebAppContext webAppContext = new WebAppContext();
      // load from standard class-loader instead of searching for WEB-INF/lib loader as we are running embedded
      webAppContext.setParentLoaderPriority(true);
      webAppContext.setContextPath("/");
      final String webAppFilePath = "/webapp/emptyService.txt";
      URL resource = ConnectorRunner.class.getResource(webAppFilePath);
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
}
