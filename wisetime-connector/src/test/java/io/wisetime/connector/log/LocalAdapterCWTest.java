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

import com.amazonaws.services.logs.AWSLogsAsync;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;
import com.github.javafaker.Faker;
import io.wisetime.connector.log.LocalAdapterCW.AwsLogsConfig;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author vadym
 */
class LocalAdapterCWTest {

  private static final Faker FAKER = new Faker();

  @Test
  void putLogs_retry() {
    final LocalAdapterCW localAdapterCW = new LocalAdapterCW();

    AWSLogsAsync awsLogsAsync = mock(AWSLogsAsync.class);
    String logMessage = FAKER.lorem().sentence();
    String expectedSequenceToken = FAKER.crypto().md5();
    final AwsLogsConfig config = new AwsLogsConfig(awsLogsAsync, Faker.instance().bothify("logGroup-#?#?#?#"),
        Faker.instance().bothify("streamName-#?#?#?#"));

    when(awsLogsAsync.putLogEvents(any())).thenAnswer(invocation -> {
      PutLogEventsRequest request = invocation.getArgument(0);
      if (!expectedSequenceToken.equals(request.getSequenceToken())) {
        throw new InvalidSequenceTokenException("Invalid sequence token").withExpectedSequenceToken(expectedSequenceToken);
      }
      return new PutLogEventsResult();
    });

    final InputLogEvent event = new InputLogEvent().withMessage(logMessage).withTimestamp(System.currentTimeMillis());
    List<InputLogEvent> logEvents = Collections.singletonList(event);

    localAdapterCW.putLogs(config, logEvents);

    //retry after InvalidSequenceTokenException
    ArgumentCaptor<PutLogEventsRequest> logEventCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);

    verify(awsLogsAsync, times(2)).putLogEvents(logEventCaptor.capture());

    PutLogEventsRequest events = logEventCaptor.getValue();
    assertThat(events.getLogEvents().size()).isEqualTo(logEvents.size());

    final InputLogEvent capturedLogEvent = logEvents.get(0);
    assertThat(capturedLogEvent).isEqualTo(event);
  }
}
