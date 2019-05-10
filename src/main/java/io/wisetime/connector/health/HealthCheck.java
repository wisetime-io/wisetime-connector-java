/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.config.ConnectorConfigKey;

/**
 * Task that will automatically stop application after 3 consecutive health check failures. Application considered to be
 * unhealthy if:
 * <p>
 * 1. For {@link ConnectorConfigKey#HEALTH_MAX_MINS_SINCE_SUCCESS} minutes there was no successful time posting processing;
 * <p>
 * 2. {@link WiseTimeConnector#isConnectorHealthy()} returns false; or
 * <p>
 * 3. Ping request to current connector instance was unsuccessful.
 *
 * @author thomas.haines@practiceinsight.io
 */
public class HealthCheck extends TimerTask {

  private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

  // visible for testing
  static final int MAX_SUCCESSIVE_FAILURES = 3;

  private final HealthIndicator[] healthIndicators;
  private final AtomicInteger failureCount;
  private Runnable shutdownFunction = () -> System.exit(1);

  public HealthCheck(HealthIndicator... healthIndicators) {
    this.failureCount = new AtomicInteger(0);
    this.healthIndicators = healthIndicators;
  }

  @Override
  public void run() {
    boolean healthy = checkConnectorHealth();
    if (healthy) {
      failureCount.set(0);
      log.debug("Health check successful");
    } else {
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

  public boolean checkConnectorHealth() {
    try {
      for (HealthIndicator healthIndicator : healthIndicators) {
        if (!healthIndicator.isHealthy()) {
          log.warn("Connector is unhealthy. {} is in failed state", healthIndicator.name());
          return false;
        }
      }
      return true;
    } catch (Throwable t) {
      log.error("Unhealthy state where exception occurred checking health, returning unhealthy; msg='{}'",
          t.getMessage(), t);
      return false;
    }
  }

  /**
   * Set function to be executed when connector found to be unhealthy.
   */
  public HealthCheck setShutdownFunction(Runnable shutdownFunction) {
    this.shutdownFunction = shutdownFunction;
    return this;
  }

  // visible for testing
  int getFailureCount() {
    return failureCount.get();
  }
}
