/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class NoOpTagSlowLoopRunnerTest {

  private TagSlowLoopRunner tagSlowLoopRunner;

  @BeforeEach
  void setup() {
    tagSlowLoopRunner = new NoOpTagSlowLoopRunner();
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = tagSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> tagSlowLoopRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(tagSlowLoopRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(tagSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    tagSlowLoopRunner.lastSuccessfulRun = new DateTime().minusYears(1);

    assertThat(tagSlowLoopRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
