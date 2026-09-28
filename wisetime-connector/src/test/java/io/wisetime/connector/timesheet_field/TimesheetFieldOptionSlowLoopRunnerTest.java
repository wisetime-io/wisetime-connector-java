/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.timesheet_field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.WiseTimeConnector;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author zeynep.aktas
 */
class TimesheetFieldOptionSlowLoopRunnerTest {

  private WiseTimeConnector connector;
  private TimesheetFieldOptionSlowLoopRunner timesheetFieldOptionSlowLoopRunner;

  @BeforeEach
  void setup() {
    connector = mock(WiseTimeConnector.class);
    timesheetFieldOptionSlowLoopRunner = new TimesheetFieldOptionSlowLoopRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    ZonedDateTime startRun = timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);
    timesheetFieldOptionSlowLoopRunner.run();

    assertThat(timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun.toInstant().toEpochMilli())
        .as("expect last success was updated")
        .isGreaterThan(startRun.toInstant().toEpochMilli());

    verify(connector, times(1)).performTimesheetFieldOptionUpdateSlowLoop();
  }

  @Test
  void testIsHealthy() {
    assertThat(timesheetFieldOptionSlowLoopRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    timesheetFieldOptionSlowLoopRunner.lastSuccessfulRun = ZonedDateTime.now().minusYears(1);
    assertThat(timesheetFieldOptionSlowLoopRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }

}
