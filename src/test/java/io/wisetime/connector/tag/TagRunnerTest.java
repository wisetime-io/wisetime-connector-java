/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import org.joda.time.DateTime;
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

  @Test
  void testRun() throws Exception {
    WiseTimeConnector connector = mock(WiseTimeConnector.class);
    TagRunner tagRunner = new TagRunner(connector);
    DateTime startRun = tagRunner.getLastSuccessfulRun();
    Thread.sleep(1);
    tagRunner.run();

    assertThat(tagRunner.getLastSuccessfulRun())
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(connector, times(1)).performTagUpdate();
  }
}