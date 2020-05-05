/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import com.google.common.collect.ImmutableList;

import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroupStatus.StatusEnum;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  private FakeEntities fakeEntities = new FakeEntities();

  @BeforeEach
  void setup() {
    apiClientMock = mock(ApiClient.class);
    wiseTimeConnectorMock = mock(WiseTimeConnector.class);
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    fetchClient = new FetchClientTimePoster(wiseTimeConnectorMock, apiClientMock,
        mock(HealthCheck.class), timeGroupIdStoreMock, 25);
  }

  @Test
  void testStartStop() throws Exception {
    when(apiClientMock.fetchTimeGroups( anyInt())).thenReturn(ImmutableList.of());
    fetchClient.start();
    assertThat(fetchClient.isRunning()).isTrue();
    assertThat(fetchClient.isHealthy()).isTrue();
    fetchClient.stop();
    assertThat(fetchClient.isRunning()).isFalse();
  }

  @Test
  void successfulTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.SUCCESS());
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.SUCCESS);
  }

  @Test
  void successfulTimeGroup_retry() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.SUCCESS());
    // should still work thanks to RetryPolicy
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenThrow(new IOException())
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(11000);
    fetchClient.stop();
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.SUCCESS);
  }

  @Test
  void previouslySuccessfulTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("PERMANENT_FAILURE"));
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
    verify(wiseTimeConnectorMock, times(0)).postTime(any(), any());
  }

  @Test
  void failedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(timeGroupIdStoreMock.getPostStatusForFetchClient(timeGroup.getGroupId()))
        .thenReturn(Optional.of("IN_PROGRESS"));
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.PERMANENT_FAILURE());
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
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
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.RETRIABLE_FAILURE);
  }


  @Test
  void alreadySeenTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.of("SUCCESS"));
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());

    fetchClient.start();
    while(fetchClient.isActive()) {
      log.trace("waiting for task completion");
    }
    Thread.sleep(100);
    fetchClient.stop();

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(StatusEnum.SUCCESS);
  }

  @Test
  void alreadySeenTimeGroup_inProgress() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeenFetchClient(timeGroup.getGroupId())).thenReturn(Optional.of("IN_PROGRESS"));
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
    verify(apiClientMock, never()).updatePostedTimeStatus(any());
  }
}
