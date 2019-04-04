/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import com.amazonaws.util.IOUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.ConsoleAppender;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.logging.DisabledMessagePublisher;
import io.wisetime.connector.logging.MessagePublisher;
import io.wisetime.connector.logging.SQLiteMessagePublisher;
import io.wisetime.connector.logging.WtEvent;
import io.wisetime.connector.logging.WtTurboFilter;
import io.wisetime.connector.server.IntegrateWebFilter;
import io.wisetime.connector.server.TagRunner;

import static io.wisetime.connector.config.ConnectorConfigKey.DATA_DIR;
import static io.wisetime.connector.config.ConnectorConfigKey.LOCAL_DB_FILENAME;

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
  private final MessagePublisher messagePublisher;
  private final boolean runningAsMainProcess;

  @SuppressWarnings("ParameterNumber")
  private ServerRunner(Server server,
                       int port,
                       WebAppContext webAppContext,
                       WiseTimeConnector wiseTimeConnector,
                       ConnectorModule connectorModule,
                       MessagePublisher messagePublisher,
                       boolean runningAsMainProcess) {
    this.server = server;
    this.port = port;
    this.webAppContext = webAppContext;
    this.wiseTimeConnector = wiseTimeConnector;
    this.connectorModule = connectorModule;
    this.messagePublisher = messagePublisher;
    this.runningAsMainProcess = runningAsMainProcess;
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
        wiseTimeConnector::isConnectorHealthy,
        messagePublisher,
        runningAsMainProcess
    );

    server.start();

    Timer healthCheckTimer = new Timer("health-check-timer");
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer");
    tagTimer.scheduleAtFixedRate(tagRunTask, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    messagePublisher.publish(new WtEvent(WtEvent.Type.SERVER_STARTED));

    while (!Thread.currentThread().isInterrupted() && (server.isStarting() || server.isRunning())) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        // ignore.
      }
    }
    server.stop();
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

    private boolean useSlf4JOnly = false;
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
      final Boolean runningAsMainProcess =
          RuntimeConfig.getBoolean(ConnectorConfigKey.RUNNING_AS_MAIN_PROCESS).orElse(true);

      final SQLiteHelper sqLiteHelper = new SQLiteHelper(getLocalDatabaseFile(persistentStorageOnly));
      final MessagePublisher messagePublisher = runningAsMainProcess ?
          new DisabledMessagePublisher() :
          new SQLiteMessagePublisher(sqLiteHelper);

      if (!useSlf4JOnly) {
        final String defaultLogXml = "/logging/logback-default.xml";
        final TurboFilter turboFilter = new WtTurboFilter(messagePublisher);
        configureStandardLogging(defaultLogXml, turboFilter);
      }

      if (apiClient == null) {
        if (StringUtils.isEmpty(apiKey)) {
          throw new IllegalArgumentException(
              "an apiKey must be supplied via constructor or environment parameter to use the default apiClient");
        }
        RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey);
        apiClient = new DefaultApiClient(requestExecutor, messagePublisher);
      }

      if (wiseTimeConnector == null) {
        throw new IllegalArgumentException(
            String.format("an implementation of '%s' interface must be supplied", WiseTimeConnector.class.getSimpleName()));
      }

      IntegrateApplication sparkApp = new IntegrateApplication(wiseTimeConnector, messagePublisher);

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
      if (!StringUtils.isBlank(shutdownToken)) {
        handlerCollection.addHandler(new ShutdownHandler(shutdownToken, false, true));
      }
      server.setHandler(handlerCollection);

      addCustomizers(port, server);

      ConnectorModule connectorModule = new ConnectorModule(
          apiClient,
          new FileStore(sqLiteHelper)
      );

      return new ServerRunner(server, port, webAppContext, wiseTimeConnector, connectorModule, messagePublisher,
          runningAsMainProcess);
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

    @SuppressWarnings("SameParameterValue")
    void configureStandardLogging(String defaultLogXmlResource, TurboFilter filter) {
      Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
      LoggerContext loggerContext = rootLogger.getLoggerContext();
      loggerContext.reset();

      try {
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);


        final String userPropertyPath = RuntimeConfig
            .getString(ConnectorConfigKey.CONNECTOR_PROPERTIES_FILE)
            .orElse("");

        configurator.doConfigure(
            ServerBuilder.createDynamicJoranConfigPath(defaultLogXmlResource, userPropertyPath)
        );

      } catch (Throwable joranException) {
        // as we have removed all appenders then failed to add to context revert to console to get message to console output
        loggerContext.reset();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-5level [%thread]: %message%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        rootLogger.addAppender(consoleAppender);
        LoggerFactory.getLogger(getClass())
            .error("invalid log config in cp {} msg={}", defaultLogXmlResource, joranException.getMessage(), joranException);
      }

      loggerContext.addTurboFilter(filter);
    }

    static String createDynamicJoranConfigPath(String defaultLogXmlResource, String userPropertyPath) throws IOException {
      final File diskCopyLogbackXml = Files.createTempFile("logback", ".xml").toFile();
      try (InputStream stream = ServerRunner.class.getResourceAsStream(defaultLogXmlResource)) {
        final String xmlJoran = IOUtils.toString(stream)
            .replace("runTimePropertyPath", userPropertyPath);
        FileUtils.writeStringToFile(diskCopyLogbackXml, xmlJoran, StandardCharsets.UTF_8);
        return diskCopyLogbackXml.getAbsolutePath();
      }
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

    public ServerBuilder useSlf4JOnly(boolean useSlf4JOnly) {
      this.useSlf4JOnly = useSlf4JOnly;
      return this;
    }

    private static final String DEFAULT_LOCAL_DB_FILENAME = "wisetime.sqlite";
    private static final String DEFAULT_TEMP_DIR_NAME = "wt-sqlite";

    private File getLocalDatabaseFile(boolean persistentStorageOnly) {
      final String persistentStoreDirPath = RuntimeConfig.getString(DATA_DIR).orElse(null);
      if (persistentStorageOnly && StringUtils.isBlank(persistentStoreDirPath)) {
        throw new IllegalArgumentException(String.format(
            "requirePersistentStore enabled for server -> a persistent directory must be provided using setting '%s'",
            ConnectorConfigKey.DATA_DIR.getConfigKey()));
      }

      final File persistentStoreDir = new File(StringUtils.isNotBlank(persistentStoreDirPath) ?
          persistentStoreDirPath : createTempDir().getAbsolutePath());
      if (!persistentStoreDir.exists() && !persistentStoreDir.mkdirs()) {
        throw new IllegalArgumentException(String.format("Store directory does not exist: '%s'",
            persistentStoreDir.getAbsolutePath()));
      }

      final String persistentStoreFilename = RuntimeConfig.getString(LOCAL_DB_FILENAME).orElse(DEFAULT_LOCAL_DB_FILENAME);
      return new File(persistentStoreDir, persistentStoreFilename);
    }

    private File createTempDir() {
      try {
        File tempDir = Files.createTempDirectory(DEFAULT_TEMP_DIR_NAME).toFile();
        if (!tempDir.exists()) {
          boolean mkDirResult = tempDir.mkdirs();
          if (mkDirResult) {
            // log.debug("temp dir created at {}", tempDir.getAbsolutePath());
          }
        }
        return tempDir;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
