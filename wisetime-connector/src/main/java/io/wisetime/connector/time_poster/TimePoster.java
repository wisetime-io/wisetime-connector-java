/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster;

import io.wisetime.connector.health.HealthIndicator;

/**
 * Common runner interface between webhooks and fetch clients.
 *
 * @author pascal.filippi@gmail.com
 */
public interface TimePoster extends HealthIndicator {
  /**
   * Starts the runner
   *
   * @throws Exception if any error occurred during start up
   */
  void start() throws Exception;

  /**
   * Stops the runner
   *
   * @throws Exception if any error occurred during shut down
   */
  void stop() throws Exception;
}
