/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.wisetime.connector.WiseTimeConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author thomas.haines@practiceinsight.io
 */
class TagRunnerTest {

  private WiseTimeConnector connector;
  private TagRunner tagRunner;

  @BeforeEach
   void setup() {
    connector = mock(WiseTimeConnector.class);
    tagRunner = new TagRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = tagRunner.lastSuccessfulRun;
    Thread.sleep(1);
    tagRunner.run();

    assertThat(tagRunner.lastSuccessfulRun)
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(connector, times(1)).performTagUpdate();
  }

  @Test
  void testIsHealthy() {
    assertThat(tagRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    tagRunner.lastSuccessfulRun = new DateTime().minusYears(1);
    assertThat(tagRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }
}