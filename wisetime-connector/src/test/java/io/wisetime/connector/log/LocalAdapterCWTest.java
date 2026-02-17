/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import io.wisetime.connector.log.LocalAdapterCW.AwsLogsConfig;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;

/**
 * @author vadym
 */
class LocalAdapterCWTest {

  private static final Faker FAKER = new Faker();

  @Test
  void putLogs_retry() {
    final LocalAdapterCW localAdapterCW = new LocalAdapterCW();

    CloudWatchLogsClient awsLogs = mock(CloudWatchLogsClient.class);
    String logMessage = FAKER.lorem().sentence();
    String expectedSequenceToken = FAKER.crypto().md5();
    final AwsLogsConfig config = new AwsLogsConfig(awsLogs, Faker.instance().bothify("logGroup-#?#?#?#"),
        Faker.instance().bothify("streamName-#?#?#?#"));

    when(awsLogs.putLogEvents(any(PutLogEventsRequest.class))).thenAnswer(invocation -> {
      PutLogEventsRequest request = invocation.getArgument(0);
      if (!expectedSequenceToken.equals(request.sequenceToken())) {
        throw InvalidSequenceTokenException.builder()
            .message("Invalid sequence token")
            .expectedSequenceToken(expectedSequenceToken)
            .build();
      }
      return PutLogEventsResponse.builder().build();
    });

    final InputLogEvent event = InputLogEvent.builder()
        .message(logMessage)
        .timestamp(System.currentTimeMillis())
        .build();
    List<InputLogEvent> logEvents = Collections.singletonList(event);

    localAdapterCW.putLogs(config, logEvents);

    //retry after InvalidSequenceTokenException
    ArgumentCaptor<PutLogEventsRequest> logEventCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

    verify(awsLogs, times(2)).putLogEvents(logEventCaptor.capture());

    PutLogEventsRequest events = logEventCaptor.getValue();
    assertThat(events.logEvents().size()).isEqualTo(logEvents.size());

    final InputLogEvent capturedLogEvent = logEvents.get(0);
    assertThat(capturedLogEvent).isEqualTo(event);
  }
}
