/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import io.wisetime.connector.metric.MetricInfo;

/**
 * Provides the ability to manage connector (e.g. start/stop) and check its work (e.g. health and metrics)
 *
 * @author yehor.lashkul
 */
public interface ConnectorController {

  /**
   * Starts the connector.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   *
   * @throws Exception if any error occurred during start up
   */
  void start() throws Exception;

  /**
   * Stops previously run connector.
   *
   * @throws Exception if any error occurred during shut down
   */
  void stop() throws Exception;

  /**
   * Checks if connector is healthy.
   *
   * @return whether the connector is healthy
   */
  boolean isHealthy();

  /**
   * Returns metrics collected from the start of the application.
   *
   * @return {@link MetricInfo} object as a representation of the collected metrics
   */
  MetricInfo getMetrics();
}
