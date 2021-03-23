/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.ResourceNotFoundException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.Data;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author thomas.haines
 */
class LocalAdapterCW implements LoggingBridge {

  private final ConcurrentLinkedQueue<LogQueueCW.LogEntryCW> messageQueue = new ConcurrentLinkedQueue<>();

  private final LogQueueCW logQueueCW = new LogQueueCW();
  private AwsLogsConfig awsLogWrapper;

  /**
   * Initialise this instance of {@code LocalAdapterCW}.
   */
  void init(ManagedConfigResponse config) {
    Preconditions.checkArgument(config != null, "ManageConfigResponse is required!");
    awsLogWrapper = createLocalConfigLogger(config);
  }

  @Override
  public void writeMessage(LogQueueCW.LogEntryCW logEntryCW) {
    if (awsLogWrapper.isValid()) {
      messageQueue.offer(logEntryCW);
    }
  }

  @Override
  public void flushMessages() {
    if (messageQueue.isEmpty()) {
      return;
    }
    processLogEntries();
  }

  /**
   * Send log entries.
   */
  private synchronized void processLogEntries() {
    final AwsLogsConfig currentLogsConfig = this.awsLogWrapper;
    if (!currentLogsConfig.isValid()) {
      return;
    }
    boolean sentLimit;
    do {
      sentLimit = processToLimit(currentLogsConfig);
    } while (sentLimit);
  }

  private boolean processToLimit(AwsLogsConfig awsLogsConfig) {
    // process up to X messages per POST

    final LogQueueCW.PutLogEventList logEventResult = logQueueCW.createListFromQueue(messageQueue);
    List<InputLogEvent> eventList = logEventResult.getEventList();

    if (!eventList.isEmpty()) {
      // The log events in the batch must be in chronological ordered by their time stamp.
      List<InputLogEvent> eventListSorted =
          eventList.stream()
              .sorted(Comparator.comparingLong(InputLogEvent::getTimestamp))
              .collect(Collectors.toList());

      // send sorted group to cloud watch
      putLogs(awsLogsConfig, eventListSorted);
    }
    return logEventResult.isLimitReached();
  }

  @VisibleForTesting
  void putLogs(AwsLogsConfig awsLog, List<InputLogEvent> events) {
    try {
      final PutLogEventsResult result = awsLog.getAwsLogs().putLogEvents(
          new PutLogEventsRequest()
              .withLogGroupName(awsLog.getLogGroupName())
              .withLogStreamName(awsLog.getLogStreamName())
              .withLogEvents(events)
              .withSequenceToken(awsLog.getCloudWatchNextSequenceToken()));

      awsLog.setCloudWatchNextSequenceToken(result.getNextSequenceToken());

    } catch (InvalidSequenceTokenException e) {
      System.err.println("Invalid AWS sequence token detected");
      awsLog.setCloudWatchNextSequenceToken(e.getExpectedSequenceToken());
      putLogs(awsLog, events);
    }
  }

  private AwsLogsConfig createLocalConfigLogger(final ManagedConfigResponse config) {
    final Optional<AWSCredentials> awsCredentials = lookupCredentials(config);
    if (!awsCredentials.isPresent()) {
      System.err.println("AWS credentials not found, AWS logger disabled");
      return AwsLogsConfig.noConfig();
    }

    Preconditions.checkArgument(config.getGroupName() != null, "GroupName is required!");

    final AWSLogs awsLogs = AWSLogsAsyncClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials.get()))
        .withRegion(config.getRegionName())
        .build();

    final String logStreamName = generateLogStreamName();

    try {
      awsLogs.createLogStream(
          new CreateLogStreamRequest()
              .withLogGroupName(config.getGroupName())
              .withLogStreamName(logStreamName)
      );

      return new AwsLogsConfig(awsLogs, config.getGroupName(), logStreamName);

    } catch (ResourceNotFoundException ex) {
      System.err.println("Unable to create log stream with name "
          + logStreamName + " for a group name " + config.getGroupName() + ".");
      return AwsLogsConfig.noConfig();
    }
  }

  private String generateLogStreamName() {
    return String.format(
        "%s/%s",
        DateTime.now().withZone(DateTimeZone.UTC).toString("yyyyMMdd_HHmm"),
        UUID.randomUUID().toString());
  }

  private Optional<AWSCredentials> lookupCredentials(final ManagedConfigResponse config) {
    try {
      return Optional.of(
          new BasicSessionCredentials(config.getServiceId(), config.getServiceKey(), config.getServiceSessionToken()));

    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Data
  static class AwsLogsConfig {

    private final AWSLogs awsLogs;
    private final String logGroupName;
    private final String logStreamName;

    /**
     * The basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
     * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group,
     * log stream) pair.
     */
    private String cloudWatchNextSequenceToken;

    static AwsLogsConfig noConfig() {
      return new AwsLogsConfig(null, null,"invalid");
    }

    boolean isValid() {
      return awsLogs != null;
    }
  }
}
