/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

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
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }

  public void onSuccessfulTagUpload() {
    onSuccess();
  }
}
