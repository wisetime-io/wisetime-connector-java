/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class around the tag slow sync process, that enforces a singleton runner pattern in the event that the previous
 * upload process has not completed prior to the next scheduled check.
 *
 * @author pascal
 */
@Slf4j
public class TagSlowLoopRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public TagSlowLoopRunner(WiseTimeConnector connector) {
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performTagUpdateSlowLoop();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }
}
