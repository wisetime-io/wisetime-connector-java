/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.wisetime.connector.postedtime.webhook.WebhookApplication;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;

/**
 * Task that will automatically stop application after 3 consecutive health check failures. Application considered to be
 * unhealthy if:
 * <p>
 * 1. For {@link ConnectorConfigKey#HEALTH_MAX_MINS_SINCE_SUCCESS} minutes there was no successful time posting processing;
 * <p>
 * 2. {@link WiseTimeConnector#isConnectorHealthy()} returns false;
 * or
 * <p>
 * 3. Ping request to current connector instance was unsuccessful.
 *
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public class HealthCheck extends TimerTask {

  private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

  // Visible for testing
  static final int MAX_SUCCESSIVE_FAILURES = 3;
  static final int MAX_MINS_SINCE_SUCCESS_DEFAULT = 60;

  private final int webhookPort;
  private final Executor executor = Executor.newInstance();
  private final Supplier<DateTime> lastRunSuccess;
  private final Supplier<Boolean> connectorHealthCheck;
  private final AtomicInteger failureCount;
  private final int maxMinsSinceSuccess;

  // Modifiable for testing
  private int latencyTolerance;
  private Runnable shutdownFunction;

  /**
   * Create health check that doesn't check webhook.
   *
   * @param lastRunSuccess       Provides the last time the tag upsert method was run.
   * @param connectorHealthCheck Optionally, the connector implementation can provide a health check via this function.
   */
  public HealthCheck(Supplier<DateTime> lastRunSuccess, Supplier<Boolean> connectorHealthCheck) {
    this(0, lastRunSuccess, connectorHealthCheck);
  }

  /**
   * Create health check that also checks a configured webhook.
   *
   * @param webhookPort          The webhookPort that webhook server is running on in this VM.
   * @param lastRunSuccess       Provides the last time the tag upsert method was run.
   * @param connectorHealthCheck Optionally, the connector implementation can provide a health check via this function.
   */
  public HealthCheck(int webhookPort, Supplier<DateTime> lastRunSuccess, Supplier<Boolean> connectorHealthCheck) {
    this.webhookPort = webhookPort;
    this.lastRunSuccess = lastRunSuccess;
    this.connectorHealthCheck = connectorHealthCheck;
    this.failureCount = new AtomicInteger(0);
    this.shutdownFunction = () -> {
      try {
        // Allow time for logs to reach AWS before killing VM
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        log.info(e.getMessage());
      }
      System.exit(-1);
    };
    this.latencyTolerance = 2000;
    this.maxMinsSinceSuccess = RuntimeConfig
        .getInt(ConnectorConfigKey.HEALTH_MAX_MINS_SINCE_SUCCESS)
        .orElse(MAX_MINS_SINCE_SUCCESS_DEFAULT);
  }

  @Override
  public void run() {
    boolean healthy = checkServerHealth();
    if (healthy) {
      failureCount.set(0);
      log.debug("Health check successful");
    } else {
      // Increment fail count, and if more than 3 successive errors, call shutdown function
      if (failureCount.incrementAndGet() >= MAX_SUCCESSIVE_FAILURES) {
        log.error("After {} successive errors, VM is assumed unhealthy, exiting", MAX_SUCCESSIVE_FAILURES);
        shutdownFunction.run();
      } else {
        log.warn("Number of successive health failures={}", failureCount.get());
      }
    }
  }

  private boolean checkServerHealth() {
    try {
      final DateTime lastSuccessResult = lastRunSuccess.get();
      if (DateTime.now().minusMinutes(maxMinsSinceSuccess).isAfter(lastSuccessResult)) {
        log.info(
            "Unhealthy state where lastRunSuccess ({}) is not within the last {}mins (maxMinutesSinceSuccess)",
            lastSuccessResult,
            maxMinsSinceSuccess
        );
        return false;
      }
      if (Boolean.FALSE.equals(connectorHealthCheck.get())) {
        log.info("connectorHealthCheck returned false");
        return false;
      }
      if (webhookPort != 0) {
        // Webhook port is configured and is non-ephemeral
        return checkWebhookHealth();
      }
      return true;

    } catch (Throwable t) {
      log.error("Exception occurred checking health, returning unhealthy; msg='{}'", t.getMessage(), t);
      return false;
    }
  }

  private boolean checkWebhookHealth() throws IOException {
    log.debug("Calling local endpoint over HTTP to check whether server is responding");
    // Call health endpoint to check responding & status of 200 re response
    String result = executor.execute(
        Request.Get(String.format("http://localhost:%d/ping", webhookPort))
            .connectTimeout(latencyTolerance)
            .socketTimeout(latencyTolerance))
        .returnContent().asString();

    return WebhookApplication.PING_RESPONSE.equals(result);
  }

  // Visible for testing
  HealthCheck setShutdownFunction(Runnable shutdownFunction) {
    this.shutdownFunction = shutdownFunction;
    return this;
  }

  // Visible for testing
  HealthCheck setLowLatencyTolerance() {
    this.latencyTolerance = 100;
    return this;
  }

  // Visible for testing
  int getFailureCount() {
    return failureCount.get();
  }
}
