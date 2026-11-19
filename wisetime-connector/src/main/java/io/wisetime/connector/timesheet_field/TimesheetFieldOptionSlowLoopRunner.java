/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.timesheet_field;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class around the timesheet field option slow sync process, that enforces a singleton runner pattern in the
 * event that the previous upload process has not completed prior to the next scheduled check.
 *
 * @author zeynep.aktas
 */
public class TimesheetFieldOptionSlowLoopRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public TimesheetFieldOptionSlowLoopRunner(WiseTimeConnector connector) {
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performTimesheetFieldOptionUpdateSlowLoop();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }
}
