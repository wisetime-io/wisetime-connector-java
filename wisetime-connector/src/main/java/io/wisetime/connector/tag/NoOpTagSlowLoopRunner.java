/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import lombok.extern.slf4j.Slf4j;

/**
 * No op tag runner.
 *
 * @see io.wisetime.connector.config.ConnectorConfigKey#TAG_SCAN
 * @author vadym
 */
@Slf4j
public class NoOpTagSlowLoopRunner extends TagSlowLoopRunner {

  public NoOpTagSlowLoopRunner() {
    super(null);
  }

  @Override
  public void run() {

  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
