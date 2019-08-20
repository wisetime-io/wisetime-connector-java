/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
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
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class LocalAdapterCWTest {

  private static final Faker FAKER = new Faker();

  @Test
  void putLogs_retry() {
    final LocalAdapterCW localAdapterCW = mock(LocalAdapterCW.class);

    AWSLogsAsync awsLogsAsync = mock(AWSLogsAsync.class);
    doCallRealMethod().when(localAdapterCW).putLogs(any(), any(), any());
    String logName = FAKER.name().name();
    String logMessage = FAKER.lorem().sentence();
    String expectedSequenceToken = FAKER.crypto().md5();

    when(awsLogsAsync.putLogEvents(any())).thenAnswer(invocation -> {
      PutLogEventsRequest request = invocation.getArgument(0);
      if (!expectedSequenceToken.equals(request.getSequenceToken())) {
        throw new InvalidSequenceTokenException("Invalid sequence token").withExpectedSequenceToken(expectedSequenceToken);
      }
      return new PutLogEventsResult();
    });

    InputLogEvent event = new InputLogEvent().withMessage(logMessage).withTimestamp(System.currentTimeMillis());
    localAdapterCW.putLogs(awsLogsAsync, logName, Collections.singletonList(event));

    //retry after InvalidSequenceTokenException
    verify(awsLogsAsync, times(2)).putLogEvents(any());
  }
}