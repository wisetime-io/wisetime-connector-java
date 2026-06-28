/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import ch.qos.logback.classic.Level;
import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;

/**
 * @author thomas.haines
 */
@Slf4j
class LogQueueCW {

  /**
   * <pre>
   *   a. The maximum batch size is 1,048,576 bytes, and this size is calculated as the sum of all event messages in UTF-8,
   *      plus 26 bytes for each log event.
   *   b.
   * </pre>
   *
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
      if (logEvent.message() != null) {

        int maxAwsEventSize = getMaxAwsEventSize();
        if (getEventSize(logEvent) >= maxAwsEventSize) {
          String truncatedMessage = StringUtils.truncate(logEvent.message(), maxAwsEventSize - 26);
          logEvent = logEvent.toBuilder().message(truncatedMessage).build();
          log.warn("Log message is too long. It was truncated before sending to AWS");
        }

        eventList.add(logEvent);
        if (eventList.size() >= maxLogEvents) {
          // log row limit reached
          return new PutLogEventList(eventList, true);
        }

        int logBundleSize = byteCount.addAndGet(getEventSize(logEvent));

        final int maxAwsPutSize = getMaxAwsPutSize();

        if (logBundleSize > maxAwsPutSize) {
          // message size in bytes limit reached
          return new PutLogEventList(eventList, true);
        }
      }
    }

    return new PutLogEventList(eventList, false);
  }

  @VisibleForTesting
  int getEventSize(InputLogEvent logEvent) {
    return logEvent.message().getBytes(StandardCharsets.UTF_8).length + 26;
  }

  @VisibleForTesting
  int getMaxAwsEventSize() {
    // The maximum event size is 256 KB. Please see:
    // https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/cloudwatch_limits_cwl.html
    // https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html
    final int officialAwsMaxEventSize = 256_000;

    // we set limit to ~5% less than actual limit, in case some overhead we didn't factor in max determining size
    final int conservativeLimit = 12_800;

    return officialAwsMaxEventSize - conservativeLimit;
  }

  private int getMaxAwsPutSize() {
    // The maximum batch size is 1,048,576 bytes. This size is calculated as the sum of all event messages
    // in UTF-8, plus 26 bytes for each log event. Please see:
    // https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html
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
  @AllArgsConstructor
  static class LogEntryCW {

    private final Level level;
    private final String loggerName;
    private InputLogEvent inputLogEvent;

  }
}
