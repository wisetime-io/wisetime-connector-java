/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.ActivityType;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.AddKeywordsResponse;
import io.wisetime.generated.connect.BatchUpsertTagRequest;
import io.wisetime.generated.connect.BatchUpsertTagResponse;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.SyncActivityTypesRequest;
import io.wisetime.generated.connect.SyncActivityTypesResponse;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author shane.xie@practiceinsight.io
 */
class DefaultApiClientTest {

  private RestRequestExecutor requestExecutor;
  private DefaultApiClient apiClient;

  @BeforeEach
  void init() {
    requestExecutor = mock(RestRequestExecutor.class);
    apiClient = new DefaultApiClient(requestExecutor);
  }

  @Test
  void tagUpsertBatch_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new BatchUpsertTagResponse());

    apiClient.tagUpsertBatch(fakeUpsertTagRequests(5));

    verify(requestExecutor, times(1)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.BulkTagUpsert.getClass()),
        any(BatchUpsertTagRequest.class)
    );
  }

  @Test
  void tagAddKeywordsBatch_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(), any()))
        .thenReturn(new AddKeywordsResponse());

    apiClient.tagAddKeywordsBatch(fakeAddKeywordsRequests(5));

    verify(requestExecutor, times(5)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagAddKeyword.getClass()),
        any(AddKeywordsRequest.class)
    );
  }

  @Test
  void tagAddKeywordsBatch_stops_on_error() throws IOException {
    //mockito answer is not synchronised. it is not guaranteed that only 1 return will be UpsertTagResponse on thread race
    IOException expectedException = new IOException();
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new AddKeywordsResponse())
        .thenThrow(expectedException);

    assertThatThrownBy(() -> apiClient.tagAddKeywordsBatch(fakeAddKeywordsRequests(1000)))
        .as("we expecting first requests pass and than expected exception to be thrown")
        .hasMessage("Failed to complete tag keywords upsert batch. Stopped at error.")
        .hasCause(expectedException);

    // We should notice that a request has failed way before we reach the end of the list
    // Allowance is made for requests sent in parallel before we notice an error
    // number of requests should always be less than 2*pool_size
    verify(requestExecutor, atMost(20)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagAddKeyword.getClass()),
        any(AddKeywordsRequest.class)
    );
  }

  @Test
  void tagAddKeywordsBatch_wraps_exceptions() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenThrow(new RuntimeException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagAddKeywordsBatch(fakeAddKeywordsRequests(3))
    );
  }

  @Test
  void tagDelete_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new DeleteTagResponse());

    apiClient.tagDelete(new DeleteTagRequest().name("name"));

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagDelete.getClass()),
        any(DeleteTagRequest.class)
    );
  }

  @Test
  void activityTypesSync() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(), any()))
        .thenReturn(new SyncActivityTypesResponse());

    apiClient.syncActivityTypes(new SyncActivityTypesRequest().activityTypes(fakeActivityTypes(5)));

    verify(requestExecutor, times(1)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.BatchActivityTypesUpsert.getClass()),
        any(SyncActivityTypesRequest.class)
    );
  }

  @Test
  void fetchTimeGroups() throws IOException {
    Faker faker = new Faker();
    int limit = faker.number().randomDigit();
    when(requestExecutor.executeTypedRequest(any(), any(), any()))
        .thenReturn(ImmutableList.of());

    apiClient.fetchTimeGroups(limit);

    ArgumentCaptor<List<NameValuePair>> paramCaptor = ArgumentCaptor.forClass(List.class);
    verify(requestExecutor).executeTypedRequest(
        any(),
        any(EndpointPath.PostedTimeFetch.getClass()),
        paramCaptor.capture()
    );
    assertThat(paramCaptor.getValue()).usingFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new BasicNameValuePair("limit", String.valueOf(limit))
    );
  }

  @Test
  void updatePostedTimeStatus() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new DeleteTagResponse());

    TimeGroupStatus status = new TimeGroupStatus();
    apiClient.updatePostedTimeStatus(status);

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        any(EndpointPath.PostedTimeUpdateStatus.getClass()),
        eq(status)
    );
  }

  @Test
  void healthCheckFailureNotify() throws IOException {
    HealthCheckFailureNotify request = new HealthCheckFailureNotify();
    apiClient.healthCheckFailureNotify(request);

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        eq(EndpointPath.HealthCheckFailureNotify),
        eq(request)
    );
  }

  @Test
  void healthCheckFailureRescind() throws IOException {
    apiClient.healthCheckFailureRescind();

    verify(requestExecutor).executeRequest(
        eq(EndpointPath.HealthCheckFailureRescind),
        any()
    );
  }

  private List<UpsertTagRequest> fakeUpsertTagRequests(final int numberOfRequests) {
    final ArrayList<UpsertTagRequest> requests = new ArrayList<>();
    IntStream
        .range(1, numberOfRequests + 1)
        .forEach(i -> requests.add(new UpsertTagRequest().name(String.valueOf(i))));
    return requests;
  }

  private List<AddKeywordsRequest> fakeAddKeywordsRequests(final int numberOfTags) {
    final List<AddKeywordsRequest> requests = new ArrayList<>();
    IntStream
        .range(1, numberOfTags + 1)
        .forEach(i ->
            requests.add(new AddKeywordsRequest()
                .tagName(String.valueOf(i))
                .additionalKeywords(ImmutableList.of(String.valueOf(i)))));
    return requests;
  }

  private List<ActivityType> fakeActivityTypes(final int numberOfActivityTypes) {
    final ArrayList<ActivityType> activityTypes = new ArrayList<>();
    IntStream
        .range(1, numberOfActivityTypes + 1)
        .forEach(i -> activityTypes.add(
            new ActivityType()
                .code(String.format("code_%d", i))
                .description(String.format("description_%d", i))));
    return activityTypes;
  }
}
