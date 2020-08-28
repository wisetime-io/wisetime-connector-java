/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import com.google.common.annotations.VisibleForTesting;

import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.net.URI;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.JsonPayloadService;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.time_poster.TimePoster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Wrapper around jetty server with spark application.
 *
 * @author pascal.filippi@gmail.com
 */
@Slf4j
@RequiredArgsConstructor
public class WebhookTimePoster implements TimePoster {

  private static final int LATENCY_TOLERANCE = 2000;

  private final Executor executor = Executor.newInstance();

  private final Server server;

  public WebhookTimePoster(int port, JsonPayloadService payloadService, WiseTimeConnector wiseTimeConnector,
                           MetricService metricService, SQLiteHelper sqLiteHelper) {
    this(port, payloadService, wiseTimeConnector, metricService, new TimeGroupIdStore(sqLiteHelper));
  }

  WebhookTimePoster(int port, JsonPayloadService payloadService, WiseTimeConnector wiseTimeConnector,
      MetricService metricService, TimeGroupIdStore timeGroupIdStore) {
    WebhookApplication webhookApplication = new WebhookApplication(payloadService, wiseTimeConnector,
        metricService, timeGroupIdStore);
    server = new SparkWebApp(port, webhookApplication).getServer();
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
              .connectTimeout(LATENCY_TOLERANCE)
              .socketTimeout(LATENCY_TOLERANCE))
          .returnContent().asString();
      return WebhookApplication.PING_RESPONSE.equals(result);
    } catch (IOException e) {
      log.error("Couldn't run health check: Assuming unhealthy.");
      return false;
    }
  }

  @VisibleForTesting
  public Server getServer() {
    return server;
  }
}
