/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

/**
 * Indicator of health state of connector. Constant failures may result in connector restart.
 *
 * @author vadym
 */
public interface HealthIndicator {

  /**
   * Get user friendly component name.
   */
  default String name() {
    return getClass().getSimpleName();
  }

  /**
   * Checks if the component is healthy.
   *
   * @return true if the runner is healthy, false otherwise
   */
  boolean isHealthy();
}
