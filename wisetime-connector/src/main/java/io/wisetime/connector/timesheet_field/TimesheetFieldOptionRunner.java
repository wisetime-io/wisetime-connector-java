/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.timesheet_field;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zeynep.aktas
 */
public class TimesheetFieldOptionRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public TimesheetFieldOptionRunner(WiseTimeConnector connector) {
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performTimesheetFieldOptionUpdate();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }
}
