/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.timesheet_field;

/**
 * @author zeynep.aktas
 */
public class NoOpTimesheetFieldOptionRunner extends TimesheetFieldOptionRunner {

  public NoOpTimesheetFieldOptionRunner() {
    super(null);
  }

  @Override
  public void run() {
    // no action
  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
