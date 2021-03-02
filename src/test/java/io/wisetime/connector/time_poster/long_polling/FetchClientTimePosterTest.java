/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.TimeGroupStatus.StatusEnum;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author pascal.filippi
 * @author thomas.haines
 */
@Slf4j
class FetchClientTimePosterTest {

  private FetchClientTimePoster fetchClient;

  private ApiClient apiClientMock;
  private WiseTimeConnector wiseTimeConnectorMock;
  private TimeGroupIdStore timeGroupIdStoreMock;

  private final FakeEntities fakeEntities = new FakeEntities();

  @BeforeEach
  void setup() {
    apiClientMock = mock(ApiClient.class);
    wiseTimeConnectorMock = mock(WiseTimeConnector.class);
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    final ExecutorService executorService = mock(ExecutorService.class);
    when(executorService.submit(any(Runnable.class)))
        .then(invocation -> {
          final Runnable runnable = invocation.getArgument(0);
          runnable.run();
          return null;
        });
    fetchClient = new FetchClientTimePoster(wiseTimeConnectorMock, apiClientMock,
        mock(HealthCheck.class), () -> executorService, timeGroupIdStoreMock, 25);
  }

  @Test
  void successfulTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.SUCCESS());

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.SUCCESS);
  }

  @Test
  void previouslySuccessfulTimeGroup() {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("PERMANENT_FAILURE"));

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    verify(wiseTimeConnectorMock, times(0)).postTime(any(), any());
  }

  @Test
  void failedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.PERMANENT_FAILURE());

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.FAILURE);
  }

  @Test
  void transientlyFailedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.TRANSIENT_FAILURE());

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.RETRIABLE_FAILURE);
  }


  @Test
  void alreadySeenTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.of("SUCCESS"));

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(StatusEnum.SUCCESS);
  }

  @Test
  void alreadySeenTimeGroup_inProgress() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.of("IN_PROGRESS"));

    fetchClient.processTimeGroups(Collections.singletonList(timeGroup));

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
    verify(apiClientMock, never()).updatePostedTimeStatus(any());
  }

  @Test
  void start_stop() {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(fetchClient);
    Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
      executor.shutdownNow();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    });
  }
}
