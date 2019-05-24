/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.logs.AWSLogsAsync;
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.wisetime.connector.config.RuntimeConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author thomas.haines
 */
@SuppressWarnings("Duplicates")
public class LocalAdapterCW implements LoggingBridge {
  private final ConcurrentLinkedQueue<LogEntryCW> messageQueue = new ConcurrentLinkedQueue<>();

  private AWSLogsWrapper awsLogWrapper;
  private String logGroupName = "wise-prod";

  /**
   * The basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
   * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group, log
   * stream) pair.
   */
  private String cloudWatchNextSequenceToken;

  LocalAdapterCW() {
    awsLogWrapper = createLocalConfigLogger();
  }

  @Override
  public void writeMessage(LogEntryCW logEntryCW) {
    if (awsLogWrapper.writer().isPresent()) {
      messageQueue.offer(logEntryCW);
    }
  }

  @Override
  public void flushMessages() {
    if (messageQueue.isEmpty()) {
      return;
    }

    awsLogWrapper.writer().ifPresent(awsLog -> {
      processLogEntries(awsLog, awsLogWrapper.getLogStreamName());
    });

    throw new UnsupportedOperationException();
  }

  /**
   * Send log entries
   */
  @SuppressWarnings("RightCurly")
  void processLogEntries(AWSLogsAsync awsLog, String logStreamName) {
    boolean sentLimit;
    do {
      sentLimit = processToLimit(awsLog, logStreamName);
    } while (sentLimit);
  }


  private boolean processToLimit(AWSLogsAsync awsLog, String logStreamName) {
    // process up to X messages per POST
    AtomicBoolean limitReached = new AtomicBoolean(false);

    List<InputLogEvent> eventList = createListFromQueue(limitReached);

    if (!eventList.isEmpty()) {
      // The log events in the batch must be in chronological ordered by their time stamp.
      List<InputLogEvent> eventListSorted =
          eventList.stream()
              .sorted(Comparator.comparingLong(InputLogEvent::getTimestamp))
              .collect(Collectors.toList());

      // send sorted group to cloud watch
      PutLogEventsResult result = awsLog.putLogEvents(
          new PutLogEventsRequest()
              .withLogGroupName(logGroupName)
              .withLogStreamName(logStreamName)
              .withLogEvents(eventListSorted)
              .withSequenceToken(cloudWatchNextSequenceToken)
      );
      cloudWatchNextSequenceToken = result.getNextSequenceToken();
    }
    return limitReached.get();
  }

  /**
   * <pre>
   *   a. The maximum batch size is 1,048,576 bytes, and this size is calculated as the sum of all event messages in UTF-8,
   *      plus 26 bytes for each log event.
   *   b.
   * <pre>
   * @param limitReached Set to true if limit reached
   * @return List to send to AWS
   */
  private List<InputLogEvent> createListFromQueue(AtomicBoolean limitReached) {


    final List<InputLogEvent> eventList = new ArrayList<>();
    // The maximum number of log events in a batch is 10,000.
    final int maxLogEvents = 8000;
    final AtomicInteger byteCount = new AtomicInteger();

    LogEntryCW logEntry;
    while ((logEntry = messageQueue.poll()) != null) {
      InputLogEvent logEvent = logEntry.getInputLogEvent();
      if (logEvent.getMessage() != null) {
        eventList.add(logEvent);
        if (eventList.size() >= maxLogEvents) {
          // log row limit reached
          limitReached.set(true);
          return eventList;
        }

        int logBundleSize = byteCount.addAndGet(logEvent.getMessage().getBytes(StandardCharsets.UTF_8).length + 26);
        final int maxAwsPutSize = 1_048_576 - 48_000;
        if (logBundleSize > maxAwsPutSize) {
          // message size in bytes limit reached
          limitReached.set(true);
          return eventList;
        }
      }
    }

    return eventList;
  }

  private AWSLogsWrapper createLocalConfigLogger() {
    if (RuntimeConfig.getString(() -> "DISABLE_AWS_CRED_USAGE").isPresent()) {
      return AWSLogsWrapper.noConfig();
    }
    final Optional<AWSCredentials> awsCredentials = lookupCredentials();
    if (!awsCredentials.isPresent()) {
      System.err.println("AWS credentials not found, AWS logger disabled");
      return AWSLogsWrapper.noConfig();
    }

    RuntimeConfig.setProperty(() -> "accessKeyId", awsCredentials.get().getAWSAccessKeyId());

    // proxy support possible via PredefinedClientConfigurations.defaultConfig()
    // use default config
    AWSLogsAsyncClientBuilder builder = AWSLogsAsyncClientBuilder.standard();
    builder.withRegion("ap-southeast-1");

    final AWSLogsAsync awsLog = builder.build();

    String logStreamName = String.format(
        "%s/%s",
        DateTime.now().withZone(DateTimeZone.UTC).toString("yyyyMMdd_HHmm"),
        UUID.randomUUID().toString()
    );
    final String logGroupName = getLogGroupName();

    try {
      awsLog.createLogStream(
          new CreateLogStreamRequest()
              .withLogGroupName(logGroupName)
              .withLogStreamName(logStreamName)
      );

      return new AWSLogsWrapper(awsLog, logStreamName);
    } catch (com.amazonaws.services.logs.model.ResourceNotFoundException ex) {
      System.err.println("Unable to create log stream with " +
          "a name " + logStreamName + " for a group name " + logGroupName + ".");
      return AWSLogsWrapper.noConfig();
    }
  }

  private String getLogGroupName() {
    return logGroupName;
  }

  private static Optional<AWSCredentials> lookupCredentials() {
    try {
      DefaultAWSCredentialsProviderChain credentialsChain = new DefaultAWSCredentialsProviderChain();
      return Optional.ofNullable(credentialsChain.getCredentials());
    } catch (SdkClientException sdkException) {
      return Optional.empty();
    }
  }


  @RequiredArgsConstructor
  @ToString
  private static class AWSLogsWrapper {

    private final AWSLogsAsync awsLogsAsync;

    @Getter
    private final String logStreamName;

    static AWSLogsWrapper noConfig() {
      return new AWSLogsWrapper(null, "invalid");
    }

    Optional<AWSLogsAsync> writer() {
      return Optional.ofNullable(awsLogsAsync);
    }
  }

}
