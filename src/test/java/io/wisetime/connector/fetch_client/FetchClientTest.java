package io.wisetime.connector.fetch_client;

import com.google.common.collect.ImmutableList;

import com.github.javafaker.Faker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

  private String fetchClientId;

  private Faker faker = new Faker();
  private FakeEntities fakeEntities = new FakeEntities();

  @BeforeEach
  void setup() {
    apiClientMock = mock(ApiClient.class);
    wiseTimeConnectorMock = mock(WiseTimeConnector.class);
    timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    fetchClientId = faker.numerify("fc######");
    fetchClient = new FetchClient(apiClientMock, wiseTimeConnectorMock, timeGroupIdStoreMock, fetchClientId, 25);
  }

  @Test
  void testStartStop() throws Exception {
    when(apiClientMock.fetchTimeGroups(eq(fetchClientId), anyInt())).thenReturn(ImmutableList.of());
    fetchClient.start();
    assertThat(fetchClient.isRunning()).isTrue();
    assertThat(fetchClient.isHealthy()).isTrue();
    fetchClient.stop();
    assertThat(fetchClient.isRunning()).isFalse();
  }

  @Test
  void successfulTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(false);
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.SUCCESS);
    when(apiClientMock.fetchTimeGroups(eq(fetchClientId), anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());
    verify(timeGroupIdStoreMock, times(1)).deleteTimeGroupId(eq(timeGroup.getGroupId()));

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.SUCCESS);
  }

  @Test
  void failedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(false);
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.PERMANENT_FAILURE);
    when(apiClientMock.fetchTimeGroups(eq(fetchClientId), anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();
    ArgumentCaptor<TimeGroupStatus> statusCaptor = ArgumentCaptor.forClass(TimeGroupStatus.class);
    verify(apiClientMock, times(1)).updatePostedTimeStatus(statusCaptor.capture());
    verify(timeGroupIdStoreMock, times(1)).deleteTimeGroupId(eq(timeGroup.getGroupId()));

    assertThat(statusCaptor.getValue().getStatus()).isEqualTo(TimeGroupStatus.StatusEnum.FAILURE);
  }

  @Test
  void transientlyFailedTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(false);
    when(wiseTimeConnectorMock.postTime(isNull(), eq(timeGroup))).thenReturn(PostResult.TRANSIENT_FAILURE);
    when(apiClientMock.fetchTimeGroups(eq(fetchClientId), anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();

    verify(apiClientMock, never()).updatePostedTimeStatus(any());
    verify(timeGroupIdStoreMock, times(1)).deleteTimeGroupId(eq(timeGroup.getGroupId()));
  }

  @Test
  void alreadySeenTimeGroup() throws Exception {
    TimeGroup timeGroup = fakeEntities.randomTimeGroup();
    when(timeGroupIdStoreMock.alreadySeen(timeGroup.getGroupId())).thenReturn(true);
    when(apiClientMock.fetchTimeGroups(eq(fetchClientId), anyInt()))
        .thenReturn(ImmutableList.of(timeGroup))
        .thenReturn(ImmutableList.of());
    fetchClient.start();
    Thread.sleep(100);
    fetchClient.stop();

    verify(wiseTimeConnectorMock, never()).postTime(isNull(), any());
    verify(apiClientMock, never()).updatePostedTimeStatus(any());
  }
}
