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
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import ch.qos.logback.core.ConsoleAppender;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.ConnectorStore;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.server.IntegrateWebFilter;
import io.wisetime.connector.server.TagRunner;
import io.wisetime.connector.template.TemplateFormatter;
import io.wisetime.connector.template.TemplateFormatterConfig;

/**
 * Main entry point of WiseTime connector. Instance of service is created by {@link ServerBuilder}:
 *
 *     ServerRunner runner = ServerRunner.createServerBuilder()
 *         .withWiseTimeConnector(myConnector)
 *         .withApiKey(apiKey)
 *         .build();
 *
 * You can then start the server by calling {@link #startServer()}. This is a blocking call, meaning that the current thread
 * will wait till the application dies.
 *
 * Main extension point is {@link WiseTimeConnector}.
 *
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
   *
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   */
  public void startServer() throws Exception {
    initWiseTimeConnector();
    final TagRunner tagRunTask = new TagRunner(wiseTimeConnector::performTagUpdate);
    final HealthCheck healthRunner = new HealthCheck(
        getPort(),
        tagRunTask::getLastSuccessfulRun,
        wiseTimeConnector::isConnectorHealthy
    );

    Timer healthCheckTimer = new Timer("health-check-timer");
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer");
    tagTimer.scheduleAtFixedRate(tagRunTask, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    server.start();
    server.join();
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
   *
   * You have to set a connector by calling {@link #withWiseTimeConnector(WiseTimeConnector)}.
   */
  @SuppressWarnings("UnusedReturnValue")
  public static class ServerBuilder {

    private int port = 8080;
    private boolean useSlf4JOnly = false;
    private boolean persistentStorageOnly = false;
    private WiseTimeConnector wiseTimeConnector;
    private ApiClient apiClient;
    private String apiKey;
    private TemplateFormatterConfig.Builder templateConfigBuilder = TemplateFormatterConfig.builder();

    /**
     * Build {@link ServerRunner}. Make sure to set {@link WiseTimeConnector} and apiKey or apiClient before calling
     * this method.
     */
    public ServerRunner build() {

      if (!useSlf4JOnly) {
        final String defaultLogXml = "/logging/logback-default.xml";
        configureStandardLogging(defaultLogXml);
      }

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
      Server server = new Server(getPort());

      WebAppContext webAppContext = createWebAppContext();

      webAppContext.addFilter(
          new FilterHolder(new IntegrateWebFilter(sparkApp)),
          "/*",
          null
      );

      server.setHandler(webAppContext);
      addCustomizers(getPort(), server);

      ConnectorModule connectorModule = new ConnectorModule(
          apiClient,
          new TemplateFormatter(templateConfigBuilder.build()),
          createStore(persistentStorageOnly)
      );

      return new ServerRunner(server, port, webAppContext, wiseTimeConnector, connectorModule);
    }

    private ConnectorStore createStore(boolean persistenceRequired) {
      String persistentStoreDir = RuntimeConfig.getString(ConnectorConfigKey.DATA_DIR).orElse(null);
      if (persistenceRequired && persistentStoreDir == null) {
        throw new IllegalArgumentException(String.format(
            "requirePersistentStore enabled for server -> a persistent directory must be provided using setting '%s'",
            ConnectorConfigKey.DATA_DIR.getConfigKey()));
      }
      return new FileStore(persistentStoreDir);
    }

    /**
     * Implementation of {@link WiseTimeConnector} is required to start server.
     */
    public ServerBuilder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector) {
      this.wiseTimeConnector = wiseTimeConnector;
      return this;
    }

    /**
     * More information about WiseTime API key: <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
     * You have to provide api key or custom implementation of {@link ApiClient} for successful authorization.
     * @see #withApiClient(ApiClient)
     */
    public ServerBuilder withApiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Whether persistent storage is required for this connector. If true, this property forces the operator to set the
     * DATA_DIR configuration parameter (via system property or environment variable). The DATA_DIR directory will be used
     * to persist data across connector restarts.
     *
     * If false, the connector will not force the operator to specify a DATA_DIR. It will still use the DATA_DIR path if
     * one is set. If none is provided, a subdirectory in the /tmp/ path will be used.
     *
     * Default is false.
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
     * @see #withApiKey(String)
     */
    public ServerBuilder withApiClient(ApiClient apiClient) {
      this.apiClient = apiClient;
      return this;
    }

    /**
     * @see TemplateFormatterConfig#DEFAULT_USE_WINCLR
     */
    public ServerBuilder withTemplateUseWinClr(boolean useWinClr) {
      templateConfigBuilder.withWindowsClr(useWinClr);
      return this;
    }

    /**
     * @see TemplateFormatterConfig#DEFAULT_TEMPLATE_PATH
     */
    public ServerBuilder withTemplatePath(String templatePath) {
      templateConfigBuilder.withTemplatePath(templatePath);
      return this;
    }

    /**
     * @see TemplateFormatterConfig#DEFAULT_MAX_LENGTH
     */
    public ServerBuilder withTemplateMaxLength(int maxLength) {
      templateConfigBuilder.withMaxLength(maxLength);
      return this;
    }

    @SuppressWarnings("SameParameterValue")
    void configureStandardLogging(String defaultLogXmlResource) {
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

    public int getPort() {
      return port;
    }

    /**
     * Set custom port server port. Default is 8080.
     */
    public ServerBuilder withPort(int port) {
      this.port = port;
      return this;
    }

    public ServerBuilder useSlf4JOnly(boolean useSlf4JOnly) {
      this.useSlf4JOnly = useSlf4JOnly;
      return this;
    }
  }
}
