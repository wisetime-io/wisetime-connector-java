/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.internal.AllProfiles;
import com.amazonaws.auth.profile.internal.BasicProfile;
import com.amazonaws.auth.profile.internal.BasicProfileConfigLoader;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.logs.AWSLogsAsync;
import com.amazonaws.services.logs.AWSLogsAsyncClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Responsible for writing log json to AWS CloudWatch.
 */
class CloudWriter {

  private final ConcurrentLinkedQueue<InputLogEvent> messageQueue = new ConcurrentLinkedQueue<>();

  private final Map<String, String> configPropertyMap;
  private final AWSLogsAsync awsLog;
  private final String logGroupName;
  private final String logStreamName;

  /**
   * Today the basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
   * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group, log
   * stream) pair.
   */
  private String cloudWatchNextSequenceToken;

  private CloudWriter(AWSLogsAsync awsLog,
                      String logGroupName,
                      String logStreamName,
                      Map<String, String> configPropertyMap) {
    this.awsLog = awsLog;
    this.logGroupName = logGroupName;
    this.logStreamName = logStreamName;
    this.configPropertyMap = configPropertyMap;
  }


  /**
   * Send log entries
   */
  @SuppressWarnings("RightCurly")
  void processLogEntries() {
    boolean sentLimit;
    do {
      sentLimit = processToLimit();
    } while (sentLimit);
  }

  void stop() {
    try {
      awsLog.shutdown();
    } catch (Exception e) {
      System.out.println("Shutdown issue with cloud writer " + e.getMessage());
    }
  }

  private boolean processToLimit() {
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

    InputLogEvent logEvent;
    while ((logEvent = messageQueue.poll()) != null) {
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

  Map<String, String> getConfigPropertyMap() {
    return configPropertyMap;
  }

  void addMessageToQueue(InputLogEvent msg) {
    messageQueue.offer(msg);
  }

  static Optional<CloudWriter> createWriter(ConfigPojo configPojo) {
    try {
      final Map<String, String> configPropertyMap = new HashMap<>();

      final Optional<AWSCredentials> awsCredentials = lookupCredentials();
      if (!awsCredentials.isPresent()) {
        System.err.println("AWS credentials not found, AWS logger disabled");
        return Optional.empty();
      }

      configPropertyMap.put("accessKeyId", awsCredentials.get().getAWSAccessKeyId());

      // proxy support possible via PredefinedClientConfigurations.defaultConfig()
      // use default config
      AWSLogsAsyncClientBuilder builder = AWSLogsAsyncClientBuilder.standard();
      configPojo.getRegion().ifPresent(regionStr -> {
        Region regionObj = RegionUtils.getRegion(regionStr);
        if (regionObj == null) {
          System.err.println(String.format("Could not find region information for '%s' in SDK metadata.", regionStr));
        } else {
          builder.withRegion(regionObj.getName());
        }
      });

      AWSLogsAsync awsLog = builder.build();

      addAWSFileProperties(configPropertyMap);

      configPojo.getPropertiesFilePath()
          .ifPresent(filePath -> configPropertyMap.putAll(addUserPropertyFile(filePath)));

      String logGroupName = configPojo
          .getLogGroupName()
          .orElseGet(() -> configPropertyMap.get("log_group_name"));

      if (logGroupName == null) {
        System.err.println(
            "Log group name is not defined, please set <logGroup> in WiseAppender config or via <propertiesFilePath>"
        );
        return Optional.empty();
      }

      String logStreamName = String.format(
          "module-%s/%s",
          configPojo.getModuleName().orElse("unknown"),
          UUID.randomUUID().toString()
      );

      try {
        awsLog.createLogStream(
            new CreateLogStreamRequest()
                .withLogGroupName(logGroupName)
                .withLogStreamName(logStreamName)
        );
        System.err.println("Streaming logs to group: " + logGroupName + ", stream: " + logStreamName);
      } catch (com.amazonaws.services.logs.model.ResourceNotFoundException ex) {
        System.err.println("Unable to create log stream with " +
            "a name " + logStreamName + " for a group name " + logGroupName + ".");
        return Optional.empty();
      }

      return Optional.of(new CloudWriter(awsLog, logGroupName, logStreamName, configPropertyMap));
    } catch (Throwable t) {
      System.err.println("AWS cloud log writer initialisation failed, msg=" + t.getMessage());
      return Optional.empty();
    }
  }

  static Map<String, String> addUserPropertyFile(String filePath) {
    final Map<String, String> configMap = new HashMap<>();
    try {
      File propertyFile = new File(filePath);
      if (propertyFile.exists()) {
        try (InputStream input = new FileInputStream(propertyFile)) {
          final Properties properties = new Properties();
          properties.load(input);
          properties.stringPropertyNames().forEach(propertyKey -> {
            configMap.put(propertyKey, properties.getProperty(propertyKey));
          });
        }
      } else {
        System.err.println("propertiesFilePath not found" + filePath);
      }
    } catch (Throwable t) {
      System.err.println("propertiesFilePath failed to initialise, msg=" + t.getMessage());
    }
    return configMap;
  }

  private static Optional<AWSCredentials> lookupCredentials() {
    try {
      DefaultAWSCredentialsProviderChain credentialsChain = new DefaultAWSCredentialsProviderChain();
      return Optional.ofNullable(credentialsChain.getCredentials());
    } catch (SdkClientException sdkException) {
      return Optional.empty();
    }
  }

  private static void addAWSFileProperties(Map<String, String> configPropertyMap) {
    File awsDir = new File(new File(System.getProperty("user.home")), ".aws");
    File configFile = new File(awsDir, "config");
    if (configFile.exists()) {
      AllProfiles allProfiles = BasicProfileConfigLoader.INSTANCE.loadProfiles(configFile);
      BasicProfile defaultProfile = allProfiles.getProfile("default");
      if (defaultProfile != null) {
        defaultProfile.getProperties().entrySet()
            .stream()
            .filter(entry -> entry.getKey() != null && !entry.getKey().toLowerCase().contains("secret"))
            .filter(entry -> !"output".equalsIgnoreCase(entry.getKey()))
            .filter(entry -> !"region".equalsIgnoreCase(entry.getKey()))
            .forEach(entry -> configPropertyMap.put(entry.getKey().toLowerCase(), entry.getValue()));
      }
    } else {
      System.out.println("Config file " + configFile.getAbsolutePath() + " doesn't exist.");
    }
  }

}
