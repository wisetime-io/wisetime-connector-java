/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.health;

import io.wisetime.connector.WiseTimeConnector;
import lombok.RequiredArgsConstructor;

/**
 * @author vadym
 */
@RequiredArgsConstructor
public class WiseTimeConnectorHealthIndicator implements HealthIndicator {

  private final WiseTimeConnector wiseTimeConnector;

  @Override
  public boolean isHealthy() {
    return wiseTimeConnector.isConnectorHealthy();
  }
}
