/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.WiseTimeConnector;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author pascal
 */
class TagSlowLoopRunnerTest {

  private WiseTimeConnector connector;
  private TagSlowLoopRunner tagSlowLoopRunner;

  @BeforeEach
   void setup() {
    connector = mock(WiseTimeConnector.class);
    tagSlowLoopRunner = new TagSlowLoopRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = tagSlowLoopRunner.lastSuccessfulRun;
    Thread.sleep(1);
    tagSlowLoopRunner.run();

    assertThat(tagSlowLoopRunner.lastSuccessfulRun)
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(connector, times(1)).performTagUpdateSlowLoop();
  }

  @Test
  void testIsHealthy() {
    assertThat(tagSlowLoopRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    tagSlowLoopRunner.lastSuccessfulRun = new DateTime().minusYears(1);
    assertThat(tagSlowLoopRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }
}