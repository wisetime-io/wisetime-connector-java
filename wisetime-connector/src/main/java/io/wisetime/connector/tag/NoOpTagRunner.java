/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
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
public class NoOpTagRunner extends TagRunner {

  public NoOpTagRunner() {
    super(null);
  }

  @Override
  public void run() {
  }

  public void onSuccessfulTagUpload() {
  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
