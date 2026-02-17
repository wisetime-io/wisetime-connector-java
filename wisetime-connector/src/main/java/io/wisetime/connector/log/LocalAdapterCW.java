/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

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
              .sorted(Comparator.comparingLong(InputLogEvent::timestamp))
              .collect(Collectors.toList());

      // send sorted group to cloud watch
      putLogs(awsLogsConfig, eventListSorted);
    }
    return logEventResult.isLimitReached();
  }

  @VisibleForTesting
  void putLogs(AwsLogsConfig awsLog, List<InputLogEvent> events) {
    try {
      final PutLogEventsResponse result = awsLog.getAwsLogs().putLogEvents(
          PutLogEventsRequest.builder()
              .logGroupName(awsLog.getLogGroupName())
              .logStreamName(awsLog.getLogStreamName())
              .logEvents(events)
              .sequenceToken(awsLog.getCloudWatchNextSequenceToken())
              .build());

      awsLog.setCloudWatchNextSequenceToken(result.nextSequenceToken());

    } catch (InvalidSequenceTokenException e) {
      System.err.println("Invalid AWS sequence token detected");
      awsLog.setCloudWatchNextSequenceToken(e.expectedSequenceToken());
      putLogs(awsLog, events);
    }
  }

  private AwsLogsConfig createLocalConfigLogger(final ManagedConfigResponse config) {
    final Optional<AwsCredentials> awsCredentials = lookupCredentials(config);
    if (!awsCredentials.isPresent()) {
      System.err.println("AWS credentials not found, AWS logger disabled");
      return AwsLogsConfig.noConfig();
    }

    Preconditions.checkArgument(StringUtils.isNotEmpty(config.getGroupName()), "GroupName is required!");

    final CloudWatchLogsClient awsLogs = CloudWatchLogsClient.builder()
        .credentialsProvider(awsCredentials::get)
        .region(Region.of(config.getRegionName()))
        .build();

    final String logStreamName = generateLogStreamName();

    try {
      awsLogs.createLogStream(
          CreateLogStreamRequest.builder()
              .logGroupName(config.getGroupName())
              .logStreamName(logStreamName)
              .build()
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
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(ZonedDateTime.now(ZoneOffset.UTC)),
        UUID.randomUUID());
  }

  private Optional<AwsCredentials> lookupCredentials(final ManagedConfigResponse config) {
    try {
      return Optional.of(AwsSessionCredentials.create(
          config.getServiceId(),
          config.getServiceKey(),
          config.getServiceSessionToken()
      ));

    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @Data
  static class AwsLogsConfig {

    private final CloudWatchLogsClient awsLogs;
    private final String logGroupName;
    private final String logStreamName;

    /**
     * The basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
     * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group,
     * log stream) pair.
     */
    private String cloudWatchNextSequenceToken;

    static AwsLogsConfig noConfig() {
      return new AwsLogsConfig(null, null, "invalid");
    }

    boolean isValid() {
      return awsLogs != null;
    }
  }
}
