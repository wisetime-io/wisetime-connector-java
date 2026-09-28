/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import com.google.common.collect.ImmutableList;
import io.wisetime.connector.api_client.support.PathParams;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.ActivityType;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.BatchUpsertTagCategoryRequest;
import io.wisetime.generated.connect.BatchUpsertTagCategoryResponse;
import io.wisetime.generated.connect.BatchUpsertTagRequest;
import io.wisetime.generated.connect.BatchUpsertTagResponse;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.SyncActivityTypesRequest;
import io.wisetime.generated.connect.SyncActivityTypesResponse;
import io.wisetime.generated.connect.SyncSession;
import io.wisetime.generated.connect.SyncTimesheetFieldOptionsRequest;
import io.wisetime.generated.connect.SyncTimesheetFieldOptionsResponse;
import io.wisetime.generated.connect.TagCategory;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.TimesheetField;
import io.wisetime.generated.connect.TimesheetFieldOption;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    apiClient.tagAddKeywordsBatch(fakeAddKeywordsRequests(5));

    verify(requestExecutor, times(5)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagAddKeyword.getClass()),
        any(AddKeywordsRequest.class)
    );
  }

  @Test
  void tagAddKeywordsBatch_stops_on_error() throws IOException {
    AtomicInteger counter = new AtomicInteger();
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenAnswer(invocation -> {
          Thread.sleep(10);
          if (counter.incrementAndGet() == 1) {
            return null;
          }
          throw new IOException();
        });

    assertThatThrownBy(() -> apiClient.tagAddKeywordsBatch(fakeAddKeywordsRequests(1000)))
        .as("we expecting first requests pass and than expected exception to be thrown")
        .hasMessage("Failed to execute tagAddKeywordsBatch");

    // check that even if some requests failed, execution continues till the end
    verify(requestExecutor, times(1000)).executeTypedBodyRequest(
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
    apiClient.tagDelete(new DeleteTagRequest().name("name"));

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagDelete.getClass()),
        any(DeleteTagRequest.class)
    );
  }

  @Test
  void tagCategoriesUpsert() throws IOException {
    TagCategory expected = new TagCategory()
        .id(Faker.instance().bothify("id-#?#?#?#"));
    when(requestExecutor.executeTypedBodyRequest(
        any(), eq(EndpointPath.BatchTagCategoryUpsert), any(BatchUpsertTagCategoryRequest.class)))
        .thenReturn(new BatchUpsertTagCategoryResponse().tagCategories(List.of(expected)));

    TagCategory tagCategory = new TagCategory()
        .externalId(Faker.instance().bothify("externalId-#?#?#?#"));
    assertThat(apiClient.tagCategoryUpsertBatch(List.of(tagCategory)))
        .as("check response from server returned")
        .containsExactly(expected);
  }

  @Test
  void activityTypesSync() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(Map.class), any()))
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
    when(requestExecutor.executeTypedRequest(any(TypeReference.class), any(), any(Map.class)))
        .thenReturn(ImmutableList.of());

    apiClient.fetchTimeGroups(limit);

    verify(requestExecutor).executeTypedRequest(
        any(),
        any(EndpointPath.PostedTimeFetch.getClass()),
        eq(Map.of("limit", limit + ""))
    );
  }

  @Test
  void updatePostedTimeStatus() throws IOException {
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

  @Test
  void listTimesheetFields() throws IOException {
    when(requestExecutor.executeTypedRequest(any(TypeReference.class), any(), any(Map.class)))
        .thenReturn(List.of(new TimesheetField().id("field-1").label("Matter Type").connected(true)));

    List<TimesheetField> fields = apiClient.listTimesheetFields();

    assertThat(fields).extracting(TimesheetField::getId).containsExactly("field-1");
    verify(requestExecutor).executeTypedRequest(
        any(),
        eq(EndpointPath.ListTimesheetFields),
        eq(Map.of())
    );
  }

  @Test
  void timesheetFieldOptionsStartSyncSession() throws IOException {
    SyncSession expected = new SyncSession().syncSessionId("session-1");
    when(requestExecutor.executeTypedRequest(eq(SyncSession.class), any(), any()))
        .thenReturn(expected);

    SyncSession result = apiClient.timesheetFieldOptionsStartSyncSession("field-1");

    assertThat(result).isEqualTo(expected);
    verify(requestExecutor).executeTypedRequest(
        eq(SyncSession.class),
        eq(EndpointPath.TimesheetFieldOptionsStartSyncSession),
        eq(PathParams.of("fieldId", "field-1"))
    );
  }

  @Test
  void timesheetFieldOptionsCompleteSyncSession() throws IOException {
    SyncSession syncSession = new SyncSession().syncSessionId("session-1");
    apiClient.timesheetFieldOptionsCompleteSyncSession("field-1", syncSession);

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        eq(EndpointPath.TimesheetFieldOptionsCompleteSyncSession),
        eq(PathParams.of("fieldId", "field-1")),
        eq(syncSession)
    );
  }

  @Test
  void timesheetFieldOptionsCancelSyncSession() throws IOException {
    SyncSession syncSession = new SyncSession().syncSessionId("session-1");
    apiClient.timesheetFieldOptionsCancelSyncSession("field-1", syncSession);

    verify(requestExecutor).executeTypedBodyRequest(
        any(),
        eq(EndpointPath.TimesheetFieldOptionsCancelSyncSession),
        eq(PathParams.of("fieldId", "field-1")),
        eq(syncSession)
    );
  }

  @Test
  void syncTimesheetFieldOptions() throws IOException {
    SyncTimesheetFieldOptionsRequest request = new SyncTimesheetFieldOptionsRequest()
        .options(List.of(new TimesheetFieldOption().code("code-1").label("label-1")));
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(PathParams.class), any()))
        .thenReturn(new SyncTimesheetFieldOptionsResponse());

    apiClient.syncTimesheetFieldOptions("field-1", request);

    verify(requestExecutor).executeTypedBodyRequest(
        eq(SyncTimesheetFieldOptionsResponse.class),
        eq(EndpointPath.BatchTimesheetFieldOptionsUpsert),
        eq(PathParams.of("fieldId", "field-1")),
        eq(request)
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
