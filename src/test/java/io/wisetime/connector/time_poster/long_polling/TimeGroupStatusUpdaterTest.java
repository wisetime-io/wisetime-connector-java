/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import com.google.common.collect.ImmutableList;

import com.github.javafaker.Faker;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.time_poster.long_polling.TimeGroupStatusUpdater;
import io.wisetime.generated.connect.TimeGroupStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author pascal.filippi@gmail.com
 */
class TimeGroupStatusUpdaterTest {

  private TimeGroupStatusUpdater timeGroupStatusUpdater;

  private  TimeGroupIdStore timeGroupIdStoreMock;
  private ApiClient apiClientMock;

  private Faker faker = new Faker();

  @BeforeEach
  void setup() {
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    apiClientMock = mock(ApiClient.class);
    timeGroupStatusUpdater = new TimeGroupStatusUpdater(timeGroupIdStoreMock, apiClientMock);
  }

  @Test
  void testRun() throws Exception {
    String id = faker.numerify("fc_######");
    PostResult postResult = PostResult.PERMANENT_FAILURE().withMessage(faker.gameOfThrones().quote());
    when(timeGroupIdStoreMock.getAllWithPendingStatusUpdate()).thenReturn(ImmutableList.of(Pair.of(id, postResult)));

    timeGroupStatusUpdater.run();

    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.FAILURE);
    assertThat(statusCaptor.getValue().getMessage()).isEqualTo(postResult.getMessage().get());
  }
}
