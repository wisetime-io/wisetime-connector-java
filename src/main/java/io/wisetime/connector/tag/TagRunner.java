/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import static io.wisetime.connector.log.LoggerNames.HEART_BEAT_LOGGER_NAME;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class around the tag upload process, that enforces a singleton runner pattern in the event that the previous
 * upload process has not completed prior to the next scheduled check.
 *
 * @author thomas.haines
 */
@Slf4j
public class TagRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public TagRunner(WiseTimeConnector connector) {
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performTagUpdate();
  }

  @Override
  protected void onSuccess() {
    super.onSuccess();
    logWiseConnectHeartbeat();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }

  public void onSuccessfulTagUpload() {
    onSuccess();
  }

  /**
   * After each successful round of checking for new tags, write to a dedicated (distinct to root logger)
   * non-additive logback appender "HEALTH", which is set for logger name parent "wt.connect.health".
   * <p>
   * The client-connector can use the logback to send a heartbeat metric, via AWS CloudWatch logs
   * (paired with using distinct logGroupName).
   */
  void logWiseConnectHeartbeat() {
    LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName()).info("WISE_CONNECT_HEARTBEAT success");
  }
}
