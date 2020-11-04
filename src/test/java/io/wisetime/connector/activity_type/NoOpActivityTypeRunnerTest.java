/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class NoOpActivityTypeRunnerTest {

  private ActivityTypeRunner activityTypeRunner;

  @BeforeEach
  void setup() {
    activityTypeRunner = new NoOpActivityTypeRunner();
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = activityTypeRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> activityTypeRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(activityTypeRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    activityTypeRunner.lastSuccessfulRun = new DateTime().minusYears(1);

    assertThat(activityTypeRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
