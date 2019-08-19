/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogGroupsResult;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.MetricTransformation;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.amazonaws.services.logs.model.PutMetricFilterRequest;
import com.amazonaws.services.logs.model.PutRetentionPolicyRequest;
import com.amazonaws.services.logs.model.ResourceNotFoundException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import spark.utils.StringUtils;

/**
 * @author thomas.haines
 */
class LocalAdapterCW implements LoggingBridge {

  // 2 month retention period
  private static final int LOG_GROUP_RETENTION_DURATION_DAYS = 60;

  private final ConcurrentLinkedQueue<LogQueueCW.LogEntryCW> messageQueue = new ConcurrentLinkedQueue<>();

  private final LogQueueCW logQueueCW = new LogQueueCW();

  private AWSLogsWrapper awsLogWrapper;

  private String logGroupName;

  /**
   * The basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
   * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group,
   * log stream) pair.
   */
  private String cloudWatchNextSequenceToken;

  /**
   * Initialise this instance of {@code LocalAdapterCW}.
   */
  void init(ManagedConfigResponse config) {
    Preconditions.checkArgument(config != null, "ManageConfigResponse is required!");
    awsLogWrapper = createLocalConfigLogger(config);
  }

  @Override
  public void writeMessage(LogQueueCW.LogEntryCW logEntryCW) {
    if (awsLogWrapper.writer().isPresent()) {
      messageQueue.offer(logEntryCW);
    }
  }

  @Override
  public void flushMessages() {
    if (messageQueue.isEmpty()) {
      return;
    }
    awsLogWrapper.writer().ifPresent(awsLog -> processLogEntries(awsLog, awsLogWrapper.getLogStreamName()));
  }

  /**
   * Send log entries.
   */
  @SuppressWarnings("RightCurly")
  private synchronized void processLogEntries(AWSLogs awsLog, String logStreamName) {
    boolean sentLimit;
    do {
      sentLimit = processToLimit(awsLog, logStreamName);
    } while (sentLimit);
  }

  private boolean processToLimit(AWSLogs awsLog, String logStreamName) {
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
      putLogs(awsLog, logStreamName, eventListSorted);
    }
    return logEventResult.isLimitReached();
  }

  @VisibleForTesting
  void putLogs(AWSLogs awsLog, String logStream, List<InputLogEvent> events) {
    try {
      final PutLogEventsResult result = awsLog.putLogEvents(
          new PutLogEventsRequest()
              .withLogGroupName(logGroupName)
              .withLogStreamName(logStream)
              .withLogEvents(events)
              .withSequenceToken(cloudWatchNextSequenceToken));

      cloudWatchNextSequenceToken = result.getNextSequenceToken();

    } catch (InvalidSequenceTokenException e) {
      System.err.println("Invalid AWS sequence token detected");
      cloudWatchNextSequenceToken = e.getExpectedSequenceToken();
      putLogs(awsLog, logStream, events);
    }
  }

  private AWSLogsWrapper createLocalConfigLogger(final ManagedConfigResponse config) {
    final Optional<AWSCredentials> awsCredentials = lookupCredentials(config);
    if (!awsCredentials.isPresent()) {
      System.err.println("AWS credentials not found, AWS logger disabled");
      return AWSLogsWrapper.noConfig();
    }

    logGroupName = config.getGroupName();
    Preconditions.checkArgument(logGroupName != null, "GroupName is required!");

    final AWSLogs awsLogs = createAWSLogs(awsCredentials.get(), config.getRegionName());
    final String logStreamName = generateLogStreamName();

    try {
      createLogGroupIfNecessary(awsLogs, logGroupName);

      awsLogs.createLogStream(
          new CreateLogStreamRequest()
              .withLogGroupName(logGroupName)
              .withLogStreamName(logStreamName)
      );

      // Creates or updates a metric filter for WISE_CONNECT_HEARTBEAT and associates it with the specified log group.
      // Metric filters allow you to configure rules to extract metric data from log events ingested through
      // PutLogEvents.
      // http://docs.aws.amazon.com/goto/WebAPI/logs-2014-03-28/PutMetricFilter
      awsLogs.putMetricFilter(new PutMetricFilterRequest()
          .withFilterName(logGroupName)
          .withLogGroupName(logGroupName)
          .withFilterPattern("WISE_CONNECT_HEARTBEAT")
          .withMetricTransformations(new MetricTransformation()
              .withMetricName(logGroupName)
              .withMetricValue("1")
              .withMetricNamespace("WiseConnectHeartBeat")
          )
      );

      return new AWSLogsWrapper(awsLogs, logStreamName);

    } catch (ResourceNotFoundException ex) {
      System.err.println("Unable to create log stream with name "
          + logStreamName + " for a group name " + logGroupName + ".");
      return AWSLogsWrapper.noConfig();
    }
  }

  private AWSLogs createAWSLogs(AWSCredentials credentials, String regionName) {
    return AWSLogsAsyncClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(regionName)
        .build();
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

  /**
   * Create log group if needed.
   */
  @VisibleForTesting
  static void createLogGroupIfNecessary(final AWSLogs awsLogs, final String logGroupName) {
    Preconditions.checkArgument(awsLogs != null, "awsLogs cannot be null!");
    Preconditions.checkArgument(StringUtils.isNotBlank(logGroupName), "logGroupName cannot be null!");

    final DescribeLogGroupsResult logGroupRes = awsLogs.describeLogGroups();

    if (logGroupRes == null || CollectionUtils.isEmpty(logGroupRes.getLogGroups())) {
      createLogGroup(awsLogs, logGroupName);

    } else {
      final boolean createLogGroup = logGroupRes.getLogGroups()
          .stream()
          .noneMatch(logGroup -> Objects.equals(logGroupName, logGroup.getLogGroupName()));

      // No log group found, so lets created one
      if (createLogGroup) {
        createLogGroup(awsLogs, logGroupName);
      }
    }
  }

  /**
   * Creates a log group with the specified name and retention policy.
   */
  private static void createLogGroup(AWSLogs awsLogs, String logGroupName) {
    awsLogs.createLogGroup(new CreateLogGroupRequest(logGroupName));

    // Set the retention of the specified log group. A retention policy retains log events in the
    // specified log group for given number of days.
    // https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutRetentionPolicy.html
    awsLogs.putRetentionPolicy(new PutRetentionPolicyRequest(
        logGroupName,
        LOG_GROUP_RETENTION_DURATION_DAYS));
  }

  @RequiredArgsConstructor
  @ToString
  private static class AWSLogsWrapper {

    private final AWSLogs awsLogs;

    @Getter
    private final String logStreamName;

    static AWSLogsWrapper noConfig() {
      return new AWSLogsWrapper(null, "invalid");
    }

    Optional<AWSLogs> writer() {
      return Optional.ofNullable(awsLogs);
    }
  }
}
