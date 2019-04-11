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

import io.wisetime.connector.webhook.WebhookApplication;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.logging.MessagePublisher;
import io.wisetime.connector.logging.WtEvent;

import static io.wisetime.connector.logging.WtEvent.Type.HEALTH_CHECK_FAILED;
import static io.wisetime.connector.logging.WtEvent.Type.HEALTH_CHECK_MAX_SUCCESSIVE_FAILURES;
import static io.wisetime.connector.logging.WtEvent.Type.HEALTH_CHECK_SUCCESS;

/**
 * Task that will automatically stop application after 3 consecutive health check failures. Application considered to be
 * unhealthy if:
 * <p>
 * 1. For {@link ConnectorConfigKey#HEALTH_MAX_MINS_SINCE_SUCCESS} minutes there was no successful time posting processing;
 * <p>
 * 2. {@link io.wisetime.connector.integrate.WiseTimeConnector#isConnectorHealthy()} returns false; or
 * <p>
 * 3. Ping request to current connector instance was unsuccessful.
 *
 * @author thomas.haines@practiceinsight.io
 */
public class HealthCheck extends TimerTask {

  private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

  // visible for testing
  static final int MAX_SUCCESSIVE_FAILURES = 3;
  static final int MAX_MINS_SINCE_SUCCESS_DEFAULT = 60;

  private final Executor executor = Executor.newInstance();
  private final Supplier<DateTime> lastRunSuccess;
  private final Supplier<Boolean> timePosterHealthCheck;
  private final Supplier<Boolean> connectorHealthCheck;
  private final AtomicInteger failureCount;
  private final int maxMinsSinceSuccess;
  private final MessagePublisher messagePublisher;

  // modifiable for testing
  private int latencyTolerance;
  private Runnable shutdownFunction;

  /**
   * @param port                 The port that server is running on in this VM.
   * @param lastRunSuccess       Provides the last time the tag upsert method was run.
   * @param connectorHealthCheck Optionally, the connector implementation can provide a health check via this function.
   */
  public HealthCheck(Supplier<DateTime> lastRunSuccess, Supplier<Boolean> timePosterHealthCheck,
                     Supplier<Boolean> connectorHealthCheck, MessagePublisher messagePublisher,
                     boolean exitOnMaxSuccessiveFailures) {
    this.timePosterHealthCheck = timePosterHealthCheck;
    this.lastRunSuccess = lastRunSuccess;
    this.connectorHealthCheck = connectorHealthCheck;
    this.failureCount = new AtomicInteger(0);
    this.messagePublisher = messagePublisher;
    this.shutdownFunction = () -> {
      messagePublisher.publish(new WtEvent(HEALTH_CHECK_MAX_SUCCESSIVE_FAILURES));
      try {
        // allow time for logs to reach AWS before killing VM
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        log.info(e.getMessage());
      }
      if (exitOnMaxSuccessiveFailures) {
        System.exit(-1);
      }
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
      messagePublisher.publish(new WtEvent(HEALTH_CHECK_SUCCESS));
    } else {
      messagePublisher.publish(new WtEvent(HEALTH_CHECK_FAILED));
      // increment fail count, and if more than {@link HealthCheck#MAX_SUCCESSIVE_FAILURES} successive errors,
      // call shutdown function
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

      if (Boolean.FALSE.equals(timePosterHealthCheck.get())) {
        log.info("Unhealthy state where timePosterHealthCheck returned false");
        return false;
      }

      if (Boolean.FALSE.equals(connectorHealthCheck.get())) {
        log.info("Unhealthy state where connectorHealthCheck returned false");
        return false;
      }
      // All checks passed
      return true;
    } catch (Throwable t) {
      log.error("Unhealthy state where exception occurred checking health, returning unhealthy; msg='{}'",
          t.getMessage(), t);
      return false;
    }
  }

  /*private boolean checkEndpointHealth() throws IOException {
    if (port == 0) {
      // running on ephemeral port, skip http check
      return true;
    }

    log.debug("Calling local endpoint over http to check server is responding");
    // call health endpoint to check responding & status of 200 re response
    String result = executor.execute(
        Request.Get(String.format("http://localhost:%d/ping", port))
            .connectTimeout(latencyTolerance)
            .socketTimeout(latencyTolerance))
        .returnContent().asString();

    return WebhookApplication.PING_RESPONSE.equals(result);
  }*/

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

  // visible for testing
  int getFailureCount() {
    return failureCount.get();
  }
}
