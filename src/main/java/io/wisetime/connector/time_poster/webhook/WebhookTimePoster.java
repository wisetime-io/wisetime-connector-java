/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import com.google.common.annotations.VisibleForTesting;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.time_poster.TimePoster;

/**
 * Wrapper around jetty server.
 *
 * @author pascal.filippi@gmail.com
 */
public class WebhookTimePoster implements TimePoster {

  private static final Logger log = LoggerFactory.getLogger(WebhookTimePoster.class);

  private final Executor executor = Executor.newInstance();
  private final int latencyTolerance = 2000;

  private final Server server;

  public WebhookTimePoster(int port, WiseTimeConnector wiseTimeConnector, MetricService metricService) {
    QueuedThreadPool pool = new QueuedThreadPool();
    pool.setDaemon(true);
    server = new Server(pool);

    WebAppContext webAppContext = createWebAppContext();
    initializeWebhookServer(port, server, webAppContext, new WebhookApplication(wiseTimeConnector, metricService));
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
                                       WebhookApplication webhookApplication) {

    webAppContext.addFilter(
        new FilterHolder(new WebhookFilter(webhookApplication)),
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

  @Override
  public void start() throws Exception {
    server.start();
  }

  @Override
  public void stop() throws Exception {
    server.stop();
  }

  @Override
  public boolean isHealthy() {
    log.debug("Calling local endpoint over http to check server is responding");
    if (!server.isRunning()) {
      return false;
    }
    try {
      URI serverUri = server.getURI();
      if (serverUri == null) {
        return false;
      }
      // call health endpoint to check responding & status of 200 re response
      String result = executor.execute(
          Request.Get(serverUri.resolve("/ping"))
              .connectTimeout(latencyTolerance)
              .socketTimeout(latencyTolerance))
          .returnContent().asString();
      return WebhookApplication.PING_RESPONSE.equals(result);
    } catch (IOException e) {
      log.error("Couldn't run health check: Assuming unhealthy.");
      return false;
    }
  }

  @Override
  public boolean isRunning() {
    return server.isStarting() || server.isRunning();
  }

  @VisibleForTesting
  public Server getServer() {
    return server;
  }
}