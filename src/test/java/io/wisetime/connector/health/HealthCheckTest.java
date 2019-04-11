/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.wisetime.connector.logging.DisabledMessagePublisher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class HealthCheckTest {

  private AtomicBoolean shutdownCalled;

  @BeforeEach
  void setup() {
    shutdownCalled = new AtomicBoolean(false);
  }

  @Test
  void testHealthy() {
    final HealthCheck healthCheck = createHealthCheck(DateTime::now);
    healthCheck.run();
    healthCheck.run();
    assertThat(healthCheck.getFailureCount())
        .as("expect server is without failure")
        .isEqualTo(0);
    assertThat(shutdownCalled.get()).isFalse();
  }

  @Test
  void testServerDown() {
    final HealthCheck healthCheck = createHealthCheck(DateTime::now, () -> Boolean.TRUE, () -> Boolean.FALSE);

    healthCheck.run();
    assertThat(healthCheck.getFailureCount())
        .as("expect server is without failure")
        .isEqualTo(1);
  }

  @Test
  void testConnectorUnhealthy_thenShutdown() {
    final HealthCheck healthCheck = createHealthCheck(DateTime::now, () -> Boolean.FALSE, () -> Boolean.TRUE);

    IntStream.range(1, HealthCheck.MAX_SUCCESSIVE_FAILURES + 1).forEach(runCount -> {
      healthCheck.run();
      assertThat(healthCheck.getFailureCount())
          .as("expect server failure count to match times called")
          .isEqualTo(runCount);

      assertThat(shutdownCalled.get())
          .as("shutdown should be called once max failures is reached")
          .isEqualTo(runCount >= HealthCheck.MAX_SUCCESSIVE_FAILURES);
    });
    assertThat(shutdownCalled.get())
        .as("if failing health ran HealthCheck.maxFailures + 1 times shutdown should have been called")
        .isTrue();
  }

  @Test
  void testConnectorUnhealthy_lastSuccessOld() {
    HealthCheck newSuccess = createHealthCheck(DateTime::now);
    newSuccess.run();
    assertThat(newSuccess.getFailureCount())
        .as("new success is healthy")
        .isEqualTo(0);

    HealthCheck oldSuccess = createHealthCheck(() ->
        DateTime.now().minusMinutes(HealthCheck.MAX_MINS_SINCE_SUCCESS_DEFAULT + 5));

    oldSuccess.run();
    assertThat(oldSuccess.getFailureCount())
        .as("old success is failure")
        .isEqualTo(1);
  }

  private HealthCheck createHealthCheck(Supplier<DateTime> lastRunSuccess) {
    return createHealthCheck(lastRunSuccess, () -> Boolean.TRUE, () -> Boolean.TRUE);
  }

  private HealthCheck createHealthCheck(Supplier<DateTime> lastRunSuccess, Supplier<Boolean> connectorHealthCheck,
                                        Supplier<Boolean> timePosterHealthCheck) {
    return new HealthCheck(lastRunSuccess, timePosterHealthCheck, connectorHealthCheck,
        new DisabledMessagePublisher(), true)
        .setLowLatencyTolerance()
        .setShutdownFunction(() -> shutdownCalled.set(true));
  }
}
