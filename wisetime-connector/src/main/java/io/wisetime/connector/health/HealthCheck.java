/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.HealthCheckFailureNotify.ErrorTypeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

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
 * @author thomas.haines
 */
@Slf4j
public class HealthCheck extends TimerTask {

  // visible for testing
  static final int MAX_SUCCESSIVE_FAILURES = 3;

  private final List<HealthIndicator> healthIndicators;
  private final AtomicInteger failureCount;
  private final ApiClient apiClient;
  private final WiseTimeConnector wiseTimeConnector;
  private Runnable shutdownFunction = () -> System.exit(1);

  public HealthCheck(ApiClient apiClient, WiseTimeConnector wiseTimeConnector) {
    this.apiClient = apiClient;
    this.wiseTimeConnector = wiseTimeConnector;
    this.failureCount = new AtomicInteger(0);
    this.healthIndicators = new ArrayList<>();
  }

  @Override
  public void run() {
    Optional<HealthCheckFailureNotify> failure = wiseTimeConnector.checkHealth();
    if (failure.isEmpty() && !checkBaseLibraryHealth()) {
      failure = Optional.of(new HealthCheckFailureNotify().errorType(ErrorTypeEnum.UNKNOWN));
    }
    if (failure.isPresent()) {
      try {
        apiClient.healthCheckFailureNotify(failure.get());
      } catch (Exception e) {
        log.error("Failed to send healthcheck failure {}", failure, e);
      }
      // increment fail count, and if more than {@link HealthCheck#MAX_SUCCESSIVE_FAILURES} successive errors,
      // call shutdown function
      if (failureCount.incrementAndGet() >= MAX_SUCCESSIVE_FAILURES) {
        log.error("After {} successive errors, Connector assumed to be unhealthy, stopping now", MAX_SUCCESSIVE_FAILURES);
        shutdownFunction.run();
      } else {
        log.warn("Number of successive health failures={}", failureCount.get());
      }
    } else {
      try {
        apiClient.healthCheckFailureRescind();
      } catch (Exception e) {
        log.error("Failed to clear error status", e);
      }
      failureCount.set(0);
      log.debug("Health check successful");
    }
  }

  public boolean checkBaseLibraryHealth() {
    try {
      boolean allHealthy = true;
      for (HealthIndicator healthIndicator : healthIndicators) {
        if (!healthIndicator.isHealthy()) {
          log.warn("Connector is unhealthy. {} is in failed state", healthIndicator.name());
          allHealthy = false;
        }
      }
      return allHealthy;
    } catch (Throwable t) {
      log.error("Unhealthy state where exception occurred checking health, returning unhealthy", t);
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

  public void addHealthIndicator(HealthIndicator... healthIndicators) {
    this.healthIndicators.addAll(Arrays.asList(healthIndicators));
  }

  // visible for testing
  int getFailureCount() {
    return failureCount.get();
  }
}
