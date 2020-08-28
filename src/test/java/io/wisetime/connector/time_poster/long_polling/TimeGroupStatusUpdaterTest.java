/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroupStatus;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author pascal.filippi@gmail.com
 */
class TimeGroupStatusUpdaterTest {

  private TimeGroupStatusUpdater timeGroupStatusUpdater;

  private TimeGroupIdStore timeGroupIdStoreMock;
  private ApiClient apiClientMock;

  private Faker faker = new Faker();

  @BeforeEach
  void setup() {
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    apiClientMock = mock(ApiClient.class);
    timeGroupStatusUpdater = new TimeGroupStatusUpdater(timeGroupIdStoreMock, apiClientMock,
        Executors::newSingleThreadExecutor);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRun_updatePostedTimeStatus() throws Exception {
    when(timeGroupIdStoreMock.getAllWithPendingStatusUpdate()).thenReturn(
        ImmutableList.of(createPendingStatus(PostResultStatus.PERMANENT_FAILURE, "permanent error")),
        ImmutableList.of(createPendingStatus(PostResultStatus.TRANSIENT_FAILURE, "transient error")),
        ImmutableList.of(createPendingStatus(PostResultStatus.SUCCESS)));
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);

    timeGroupStatusUpdater.run();

    verify(apiClientMock).updatePostedTimeStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.FAILURE);
    assertThat(statusCaptor.getValue().getMessage()).isEqualTo("permanent error");

    clearInvocations(apiClientMock);
    timeGroupStatusUpdater.run();

    verify(apiClientMock).updatePostedTimeStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.RETRIABLE_FAILURE);
    assertThat(statusCaptor.getValue().getMessage()).isEqualTo("transient error");

    clearInvocations(apiClientMock);
    timeGroupStatusUpdater.run();

    verify(apiClientMock).updatePostedTimeStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.SUCCESS);
    assertThat(statusCaptor.getValue().getMessage()).isNull();
  }

  private Pair<String, PostResult> createPendingStatus(PostResultStatus status) {
    return createPendingStatus(status, null);
  }

  private Pair<String, PostResult> createPendingStatus(PostResultStatus status, String error) {
    String id = faker.numerify("fc_######");
    PostResult postResult = PostResult.valueOf(status.name()).withMessage(StringUtils.trimToEmpty(error));
    return Pair.of(id, postResult);
  }
}
