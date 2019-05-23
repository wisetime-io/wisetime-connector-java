package io.wisetime.connector.logging.aws;

import com.amazonaws.services.logs.AWSLogsAsync;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.wisetime.connector.api_client.ApiClient;

/**
 * @author thomas.haines
 */
public class LogPersist implements Consumer<List<InputLogEvent>> {
  private static final Logger log = LoggerFactory.getLogger(LogPersist.class);

  private final AtomicReference<AwsLogger> awsLogger = new AtomicReference<>();

  LogPersist() {

//  }
//
//  LogPersist(ApiClientAccessor apiClient) {
    // TODO(TH) scheduler to get for new team info every hour, starting 1-min from nown(runCheckForAwsConfig)
    // TODO(TH) need to look at how api key bootstrap is handled
    // or we need to register as a listener for availability of apiClient changes, and request
    // copy of apiClient thereafter

    // TODO(TH) communicate re logback and programmatic configuration
    // TODO(TH) consider dependency injection using javax.inject annotation?
  }

  /**
   * Today the basic structure of the PutLogEvents API is you do a call to PutLogEvents and it returns to you a result that
   * includes the sequence number. That same sequence number must be used in the subsequent put for the same (log group, log
   * stream) pair.
   */
  private String cloudWatchNextSequenceToken;

  public void accept(List<InputLogEvent> eventList) {
    final AwsLogger awsLogger = this.awsLogger.get();
    if (awsLogger == null) {
      log.trace("No aws logger configured.");
      return;
    }

    if (!eventList.isEmpty()) {
      // The log events in the batch must be in chronological ordered by their time stamp.
      List<InputLogEvent> eventListSorted =
          eventList.stream()
              .sorted(Comparator.comparingLong(InputLogEvent::getTimestamp))
              .collect(Collectors.toList());

      // send sorted group to cloud watch
      PutLogEventsResult result = awsLogger.getAwsLog().putLogEvents(
          new PutLogEventsRequest()
              .withLogGroupName(awsLogger.getLogGroupName())
              .withLogStreamName(awsLogger.getLogStreamName())
              .withLogEvents(eventListSorted)
              .withSequenceToken(cloudWatchNextSequenceToken)
      );
      cloudWatchNextSequenceToken = result.getNextSequenceToken();
    }
  }

  public LogPersist setAwsLogger(AwsLogger awsLogger) {
    this.cloudWatchNextSequenceToken = null;
    this.awsLogger.set(awsLogger);
    return this;
  }

  public static class AwsLogger {
    private final AWSLogsAsync awsLog;
    private final String logGroupName;
    private final String logStreamName;

    public AwsLogger(AWSLogsAsync awsLog, String logGroupName, String logStreamName) {
      this.awsLog = awsLog;
      this.logGroupName = logGroupName;
      this.logStreamName = logStreamName;
    }

    public AWSLogsAsync getAwsLog() {
      return awsLog;
    }

    public String getLogGroupName() {
      return logGroupName;
    }

    public String getLogStreamName() {
      return logStreamName;
    }
  }

}
