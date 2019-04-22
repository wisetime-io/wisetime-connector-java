/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import org.eclipse.jetty.server.Server;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.wisetime.connector.ServerStartTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class HealthCheckTest {

  private static final Logger log = LoggerFactory.getLogger(HealthCheckTest.class);

  private static Server testServer;
  private static int serverPort = 0;
  private AtomicBoolean shutdownCalled;

  @BeforeAll
  static void setupServer() throws Exception {
    testServer = ServerStartTest.createTestServer();
    serverPort = ServerStartTest.getPort(testServer);
  }

  @BeforeEach
  void setup() {
    shutdownCalled = new AtomicBoolean(false);
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (testServer != null && testServer.isRunning()) {
      testServer.stop();
      log.info("server shutdown success");
    }
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
  void testServerDown() throws Exception {
    // stop server
    testServer.stop();

    final HealthCheck healthCheck = createHealthCheck(DateTime::now);
    healthCheck.run();
    assertThat(healthCheck.getFailureCount())
        .as("expect server is without failure")
        .isEqualTo(1);
  }

  @Test
  void testConnectorUnhealthy_thenShutdown() {
    final HealthCheck healthCheck = createHealthCheck(DateTime::now, () -> Boolean.FALSE);

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
    return createHealthCheck(lastRunSuccess, () -> Boolean.TRUE);
  }

  private HealthCheck createHealthCheck(Supplier<DateTime> lastRunSuccess, Supplier<Boolean> connectorHealthCheck) {
    return new HealthCheck(serverPort, lastRunSuccess, connectorHealthCheck)
        .setLowLatencyTolerance()
        .setShutdownFunction(() -> shutdownCalled.set(true));
  }
}
