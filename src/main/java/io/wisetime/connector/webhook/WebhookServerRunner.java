/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.webhook;

import com.google.common.annotations.VisibleForTesting;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import io.wisetime.connector.TimePosterRunner;

/**
 * Wrapper around jetty server to be able to provide a common runner interface between fetch clients and webhooks
 *
 * @author pascal.filippi@gmail.com
 */
public class WebhookServerRunner implements TimePosterRunner {

  private static final Logger log = LoggerFactory.getLogger(WebhookServerRunner.class);

  private final Executor executor = Executor.newInstance();
  private final int latencyTolerance;
  private final int port;

  private final Server server;

  public WebhookServerRunner(Server server, int port) {
    this.server = server;
    this.port = port;
    this.latencyTolerance = 2000;
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
    if (port == 0) {
      // running on ephemeral port, skip http check
      return true;
    }

    log.debug("Calling local endpoint over http to check server is responding");
    // call health endpoint to check responding & status of 200 re response
    String result;
    try {
      result = executor.execute(
          Request.Get(String.format("http://localhost:%d/ping", port))
              .connectTimeout(latencyTolerance)
              .socketTimeout(latencyTolerance))
          .returnContent().asString();
    } catch (IOException e) {
      log.error("Couldn't run health check: Assuming unhealthy.");
      return false;
    }

    return WebhookApplication.PING_RESPONSE.equals(result);
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
