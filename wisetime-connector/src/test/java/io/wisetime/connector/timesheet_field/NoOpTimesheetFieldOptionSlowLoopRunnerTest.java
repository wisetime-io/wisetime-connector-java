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
class NoOpTimesheetFieldOptionSlowLoopRunnerTest {

  private TimesheetFieldOptionSlowLoopRunner timesheetFieldOptionSlowLoopRunner;

  @BeforeEach
  void setup() {
    timesheetFieldOptionSlowLoopRunner = new NoOpTimesheetFieldOptionSlowLoopRunner();
  }

  @Test
  void testRun() throws Exception {
    ZonedDateTime startRun = timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> timesheetFieldOptionSlowLoopRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(timesheetFieldOptionSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun = ZonedDateTime.now().minusYears(1);

    assertThat(timesheetFieldOptionSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
