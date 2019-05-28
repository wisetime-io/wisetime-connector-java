/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.services.logs.model.InputLogEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Level;
import lombok.Data;

/**
 * @author thomas.haines
 */
class LogQueueCW {

  /**
   * <pre>
   *   a. The maximum batch size is 1,048,576 bytes, and this size is calculated as the sum of all event messages in UTF-8,
   *      plus 26 bytes for each log event.
   *   b.
   * <pre>
   * @return List to send to AWS
   */
  PutLogEventList createListFromQueue(Queue<LogEntryCW> messageQueue) {

    final List<InputLogEvent> eventList = new ArrayList<>();

    // The maximum number of log events in a batch is 10,000.
    final int maxLogEvents = 8000;
    final AtomicInteger byteCount = new AtomicInteger();

    LogQueueCW.LogEntryCW logEntry;
    while ((logEntry = messageQueue.poll()) != null) {
      InputLogEvent logEvent = logEntry.getInputLogEvent();
      if (logEvent.getMessage() != null) {
        eventList.add(logEvent);
        if (eventList.size() >= maxLogEvents) {
          // log row limit reached
          return new PutLogEventList(eventList, true);
        }

        int logBundleSize = byteCount.addAndGet(logEvent.getMessage().getBytes(StandardCharsets.UTF_8).length + 26);

        final int maxAwsPutSize = getMaxAwsPutSize();

        if (logBundleSize > maxAwsPutSize) {
          // message size in bytes limit reached
          return new PutLogEventList(eventList, true);
        }
      }
    }

    return new PutLogEventList(eventList, false);
  }

  private int getMaxAwsPutSize() {
    final int officialAwsMaxPutSize = 1_048_576;

    // we set limit to ~5% less than actual limit, in case some overhead we didn't factor in max determining size
    final int conservativeLimit = 48_000;

    return officialAwsMaxPutSize - conservativeLimit;
  }

  @Data
  static class PutLogEventList {
    private final List<InputLogEvent> eventList;

    // set to true if limit reached
    private final boolean limitReached;

  }

  /**
   * Data pojo representing a log entry for CW.
   */
  @Data
  static class LogEntryCW {

    private final Level level;
    private final String loggerName;
    private final InputLogEvent inputLogEvent;

  }
}
