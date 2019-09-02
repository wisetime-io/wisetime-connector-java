/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import static io.wisetime.connector.log.LoggerNames.HEART_BEAT_LOGGER_NAME;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

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
    LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName()).info("WISE_CONNECT_HEARTBEAT success");
  }

  public void onSuccessfulTagUpload() {
  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
