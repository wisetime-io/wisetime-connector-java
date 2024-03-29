/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class NoOpActivityTypeSlowLoopRunnerTest {

  private ActivityTypeSlowLoopRunner activityTypeSlowLoopRunner;

  @BeforeEach
  void setup() {
    activityTypeSlowLoopRunner = new NoOpActivityTypeSlowLoopRunner();
  }

  @Test
  void testRun() throws Exception {
    ZonedDateTime startRun = activityTypeSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> activityTypeSlowLoopRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(activityTypeSlowLoopRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    activityTypeSlowLoopRunner.lastSuccessfulRun = ZonedDateTime.now().minusYears(1);

    assertThat(activityTypeSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
