/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.WiseTimeConnector;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class ActivityTypeSlowLoopRunnerTest {

  private WiseTimeConnector connector;
  private ActivityTypeSlowLoopRunner activityTypeSlowLoopRunner;

  @BeforeEach
  void setup() {
    connector = mock(WiseTimeConnector.class);
    activityTypeSlowLoopRunner = new ActivityTypeSlowLoopRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = activityTypeSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);
    activityTypeSlowLoopRunner.run();

    assertThat(activityTypeSlowLoopRunner.lastSuccessfulRun)
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(connector, times(1)).performActivityTypeUpdateSlowLoop();
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeSlowLoopRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    activityTypeSlowLoopRunner.lastSuccessfulRun = new DateTime().minusYears(1);
    assertThat(activityTypeSlowLoopRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }

}
