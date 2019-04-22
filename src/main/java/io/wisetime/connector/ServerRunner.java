/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
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
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.server.IntegrateWebFilter;
import io.wisetime.connector.server.TagRunner;

/**
 * Main entry point of WiseTime connector. Instance of service is created by {@link ServerBuilder}:
 * <pre>
 *     ServerRunner runner = ServerRunner.createServerBuilder()
 *         .withWiseTimeConnector(myConnector)
 *         .withApiKey(apiKey)
 *         .build();
 *     </pre>
 *
 * You can then start the server by calling {@link #startServer()}. This is a blocking call, meaning that the current thread
 * will wait till the application dies.
 * <p>
 * Main extension point is {@link WiseTimeConnector}.
 * <p>
 * More information regarding the API key can be found here:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 *
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings("WeakerAccess")
public class ServerRunner {

  private final Server server;
  private final int port;
  private final WebAppContext webAppContext;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;

  @SuppressWarnings("ParameterNumber")
  private ServerRunner(Server server,
                       int port,
                       WebAppContext webAppContext,
                       WiseTimeConnector wiseTimeConnector,
                       ConnectorModule connectorModule) {
    this.server = server;
    this.port = port;
    this.webAppContext = webAppContext;
    this.wiseTimeConnector = wiseTimeConnector;
    this.connectorModule = connectorModule;
  }

  /**
   * Method to create new {@link ServerBuilder} instance. Automatically checks system properties for API_KEY.
   */
  public static ServerBuilder createServerBuilder() {
    ServerBuilder builder = new ServerBuilder();
    RuntimeConfig.getString(ConnectorConfigKey.API_KEY).ifPresent(builder::withApiKey);
    RuntimeConfig.getString(ConnectorConfigKey.JETTY_SERVER_SHUTDOWN_TOKEN).ifPresent(builder::withShutdownToken);
    return builder;
  }

  public int getPort() {
    return port;
  }

  @SuppressWarnings("unused")
  public WebAppContext getWebAppContext() {
    return webAppContext;
  }

  /**
   * Start the server. This will run scheduled tasks for tag updates and health checks.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   */
  public void startServer() throws Exception {
    initWiseTimeConnector();
    final TagRunner tagRunTask = new TagRunner(wiseTimeConnector::performTagUpdate);
    final HealthCheck healthRunner = new HealthCheck(
        getPort(),
        tagRunTask::getLastSuccessfulRun,
        wiseTimeConnector::isConnectorHealthy);

    Timer healthCheckTimer = new Timer("health-check-timer");
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer");
    tagTimer.scheduleAtFixedRate(tagRunTask, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    server.start();
    server.join();

    healthCheckTimer.cancel();
    healthCheckTimer.purge();
    tagTimer.cancel();
    tagTimer.purge();
  }

  //visible for testing
  void initWiseTimeConnector() {
    wiseTimeConnector.init(connectorModule);
  }

  //Visible for testing
  Server getServer() {
    return server;
  }

  /**
   * Builder for {@link ServerRunner}. You have to provide an API key or custom {@link ApiClient} that will handle
   * authentication. For more information on how to obtain an API key, refer to:
   * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
   * <p>
   * You have to set a connector by calling {@link #withWiseTimeConnector(WiseTimeConnector)}.
   */
  public static class ServerBuilder {

    private boolean persistentStorageOnly = false;
    private WiseTimeConnector wiseTimeConnector;
    private ApiClient apiClient;
    private String apiKey;
    private String shutdownToken;

    private static final int DEFAULT_WEBHOOK_PORT = 8080;

    /**
     * Build {@link ServerRunner}. Make sure to set {@link WiseTimeConnector} and apiKey or apiClient before calling this
     * method.
     */
    public ServerRunner build() {

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

      IntegrateApplication sparkApp = new IntegrateApplication(wiseTimeConnector);

      final int port = RuntimeConfig.getInt(ConnectorConfigKey.WEBHOOK_PORT).orElse(DEFAULT_WEBHOOK_PORT);

      Server server = new Server(port);

      WebAppContext webAppContext = createWebAppContext();

      webAppContext.addFilter(
          new FilterHolder(new IntegrateWebFilter(sparkApp)),
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

      final SQLiteHelper sqLiteHelper = new SQLiteHelper(persistentStorageOnly);
      ConnectorModule connectorModule = new ConnectorModule(
          apiClient,
          new FileStore(sqLiteHelper)
      );

      return new ServerRunner(server, port, webAppContext, wiseTimeConnector, connectorModule);
    }

    /**
     * Implementation of {@link WiseTimeConnector} is required to start server.
     *
     * @param wiseTimeConnector see {@link WiseTimeConnector}
     */
    public ServerBuilder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector) {
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
    public ServerBuilder withApiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * A shutdown token is required to shutdown the server externally.
     *
     * @param shutdownToken see {@link #withShutdownToken(String)}
     */
    public ServerBuilder withShutdownToken(String shutdownToken) {
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
    public ServerBuilder requirePersistentStore(boolean persistentStorageOnly) {
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
    public ServerBuilder withApiClient(ApiClient apiClient) {
      this.apiClient = apiClient;
      return this;
    }

    private WebAppContext createWebAppContext() {
      WebAppContext webAppContext = new WebAppContext();
      // load from standard class-loader instead of searching for WEB-INF/lib loader as we are running embedded
      webAppContext.setParentLoaderPriority(true);
      webAppContext.setContextPath("/");
      final String webAppFilePath = "/webapp/emptyService.txt";
      URL resource = ServerRunner.class.getResource(webAppFilePath);
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
