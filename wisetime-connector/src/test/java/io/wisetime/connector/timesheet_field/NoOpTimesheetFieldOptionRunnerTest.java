/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.timesheet_field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author zeynep.aktas
 */
class NoOpTimesheetFieldOptionRunnerTest {

  private TimesheetFieldOptionRunner timesheetFieldOptionRunner;

  @BeforeEach
  void setup() {
    timesheetFieldOptionRunner = new NoOpTimesheetFieldOptionRunner();
  }

  @Test
  void testRun() throws Exception {
    ZonedDateTime startRun = timesheetFieldOptionRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> timesheetFieldOptionRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(timesheetFieldOptionRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(timesheetFieldOptionRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    timesheetFieldOptionRunner.lastSuccessfulRun = ZonedDateTime.now().minusYears(1);

    assertThat(timesheetFieldOptionRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
