/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.fetch_client;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;

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
 * @author pascal.filippi@gmail.com
 */
class FetchClientTest {

  private FetchClient fetchClient;

  private ApiClient apiClientMock;
  private WiseTimeConnector wiseTimeConnectorMock;
  private TimeGroupIdStore timeGroupIdStoreMock;

  private FakeEntities fakeEntities = new FakeEntities();

  @BeforeEach
  void setup() {
    apiClientMock = mock(ApiClient.class);
    wiseTimeConnectorMock = mock(WiseTimeConnector.class);
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    fetchClient = new FetchClient(new FetchClientSpec(apiClientMock, wiseTimeConnectorMock, timeGroupIdStoreMock, 25));
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
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(Optional.empty());
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
  void failedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(Optional.empty());
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
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(Optional.empty());
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.TRANSIENT_FAILURE());
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();

    verify(apiClientMock, never()).updatePostedTimeStatus(any());
  }

  @Test
  void alreadySeenTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(Optional.of("SUCCESS"));
    when(apiClientMock.fetchTimeGroups(anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
  }
}
