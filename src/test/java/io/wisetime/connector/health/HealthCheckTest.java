/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
 */
class HealthCheckTest {
  private AtomicBoolean shutdownCalled;
  private HealthCheck healthCheck;

  @BeforeEach
  void setup() {
    shutdownCalled = new AtomicBoolean(false);
    healthCheck = new HealthCheck();
  }

  @Test
  void testHealthy() {
    healthCheck.addHealthIndicator(() -> true);
    healthCheck.run();
    healthCheck.run();
    assertThat(healthCheck.getFailureCount())
        .as("expect server is without failure")
        .isEqualTo(0);
    assertThat(shutdownCalled.get()).isFalse();
  }

  @Test
  void testServerDown() {
    healthCheck.addHealthIndicator(() -> false);

    healthCheck.run();
    assertThat(healthCheck.getFailureCount())
        .as("expect server is without failure")
        .isEqualTo(1);
  }

  @Test
  void testConnectorUnhealthy_thenShutdown() {
    Runnable shutdownFunction = mock(Runnable.class);
    healthCheck.addHealthIndicator(() -> false);
    healthCheck.setShutdownFunction(shutdownFunction);

    IntStream.range(0, HealthCheck.MAX_SUCCESSIVE_FAILURES).forEach(runCount -> healthCheck.run());

    verify(shutdownFunction, times(1)).run();
  }
}
