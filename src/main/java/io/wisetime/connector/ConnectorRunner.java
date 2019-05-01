/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.tag.TagRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ConnectorController}
 *
 * @author thomas.haines@practiceinsight.io, pascal.filippi@staff.wisetime.com
 */
@Slf4j
@RequiredArgsConstructor
public class ConnectorRunner implements ConnectorController {

  private final TimePosterRunner timePosterRunner;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;
  private final TagRunner tagRunner;
  private final HealthCheck healthRunner;
  private final MetricService metricService;
  private Runnable shutdownFunction;

  /**
   * Start the runner. This will run scheduled tasks for tag updates and health checks.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   */
  @Override
  public void start() throws Exception {
    initWiseTimeConnector();
    shutdownFunction = Thread.currentThread()::interrupt;
    healthRunner.setShutdownFunction(shutdownFunction);

    timePosterRunner.start();

    Timer healthCheckTimer = new Timer("health-check-timer", true);
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer", true);
    tagTimer.scheduleAtFixedRate(tagRunner, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    while (timePosterRunner.isRunning()) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        break;
      }
    }
    healthCheckTimer.cancel();
    healthCheckTimer.purge();
    tagTimer.cancel();
    tagTimer.purge();
    try {
      timePosterRunner.stop();
    } catch (Exception e) {
      log.warn("Failed to stop time poster runner", e);
    }
    wiseTimeConnector.shutdown();
  }

  @Override
  public void stop() {
    if (shutdownFunction == null) {
      throw new IllegalStateException("Connector hasn't been started");
    }
    shutdownFunction.run();
  }

  @Override
  public boolean isHealthy() {
    return healthRunner.checkConnectorHealth();
  }

  @Override
  public MetricInfo getMetrics() {
    return metricService.getMetrics();
  }

  //visible for testing
  void initWiseTimeConnector() {
    wiseTimeConnector.init(connectorModule);
  }

  TimePosterRunner getTimePosterRunner() {
    return timePosterRunner;
  }
}
