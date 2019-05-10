/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster;

/**
 * Common runner interface between webhooks and fetch clients.
 *
 * @author pascal.filippi@gmail.com
 */
public interface TimePoster {
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

  /**
   * Checks if the runner is healthy
   *
   * @return true if the runner is healthy, false otherwise
   */
  boolean isHealthy();

  /**
   * Checks if the runner is running
   *
   * @return true if the runner is running, false otherwise
   */
  boolean isRunning();
}
