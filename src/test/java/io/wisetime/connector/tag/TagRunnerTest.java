/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import io.wisetime.connector.tag.TagRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class TagRunnerTest {

  @Test
  void testRun() throws Exception {
    final AtomicBoolean runCalled = new AtomicBoolean(false);
    TagRunner tagRunner = new TagRunner(() -> runCalled.set(true));
    testRun(runCalled, tagRunner);

    // check lock does not prevent second run
    testRun(runCalled, tagRunner);
  }

  private void testRun(AtomicBoolean runCalled, TagRunner tagRunner) throws InterruptedException {
    DateTime startRun = tagRunner.getLastSuccessfulRun();
    Thread.sleep(1);
    tagRunner.run();

    assertThat(tagRunner.getLastSuccessfulRun())
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    assertThat(runCalled.get())
        .as("expect runnable called")
        .isTrue();

    runCalled.set(false);
  }
}