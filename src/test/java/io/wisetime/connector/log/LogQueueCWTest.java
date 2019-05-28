/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines
 */
class LogQueueCWTest {

  private ConcurrentLinkedQueue<LogQueueCW.LogEntryCW> messageQueue;
  private EnhancedRandom enhancedRandom;

  @BeforeEach
  void setUp() {
    messageQueue = new ConcurrentLinkedQueue<>();
    enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandom();
  }

  @Test
  void createListFromQueue() {
    final LogQueueCW logQueue = new LogQueueCW();

    // queue three thousand more items than we allow in a batch
    final int generateCount = 10_000;
    for (int i = 0; i < generateCount; i++) {
      LogQueueCW.LogEntryCW item = enhancedRandom.nextObject(LogQueueCW.LogEntryCW.class);
      messageQueue.add(item);
    }

    int attemptCount = 0;
    int numberProcessed = 0;

    LogQueueCW.PutLogEventList lastResult = logQueue.createListFromQueue(messageQueue);
    assertThat(lastResult.isLimitReached())
        .as("expect limit reached on first pass")
        .isTrue();
    numberProcessed += lastResult.getEventList().size();

    final int maxAttemptCount = 100;
    while (!messageQueue.isEmpty() && attemptCount < maxAttemptCount) {
      lastResult = logQueue.createListFromQueue(messageQueue);
      numberProcessed += lastResult.getEventList().size();
      attemptCount++;
    }

    assertThat(lastResult.isLimitReached())
        .as("expect last pass does not reach limit")
        .isFalse();

    assertThat(numberProcessed)
        .as("expect to obtain same number as added")
        .isEqualTo(generateCount);

    assertThat(attemptCount)
        .as("do not expect to hit attempt count")
        .isLessThan(maxAttemptCount);

  }

}
