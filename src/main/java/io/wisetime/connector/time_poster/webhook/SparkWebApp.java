/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import java.io.File;
import java.net.URL;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import spark.servlet.SparkApplication;

/**
 * Jetty server wrapper with proper spark application initialization in servlet mode.
 *
 * @author yehor.lashkul
 */
public class SparkWebApp {

  private final Server server;

  public SparkWebApp(int port, SparkApplication sparkApplication) {
    QueuedThreadPool pool = new QueuedThreadPool();
    pool.setDaemon(true);
    server = new Server(pool);

    WebAppContext webAppContext = createWebAppContext();
    initializeWebhookServer(port, server, webAppContext, sparkApplication);
  }

  public Server getServer() {
    return server;
  }

  private WebAppContext createWebAppContext() {
    WebAppContext webAppContext = new WebAppContext();
    // load from standard class-loader instead of searching for WEB-INF/lib loader as we are running embedded
    webAppContext.setParentLoaderPriority(true);
    webAppContext.setContextPath("/");
    final String webAppFilePath = "webapp/emptyService.txt";
    URL resource = this.getClass().getClassLoader().getResource(webAppFilePath);
    if (resource == null) {
      throw new RuntimeException("should find file as base of webapp " + webAppFilePath);
    }
    webAppContext.setResourceBase(new File(resource.getFile()).getParent());

    return webAppContext;
  }

  private void initializeWebhookServer(int port,
                                       Server server,
                                       WebAppContext webAppContext,
                                       SparkApplication sparkApplication) {
    webAppContext.addFilter(
        new FilterHolder(new WebhookFilter(sparkApplication)),
        "/*",
        null
    );

    HandlerCollection handlerCollection = new HandlerCollection(false);
    handlerCollection.addHandler(webAppContext);
    server.setHandler(handlerCollection);

    addCustomizers(port, server);
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
