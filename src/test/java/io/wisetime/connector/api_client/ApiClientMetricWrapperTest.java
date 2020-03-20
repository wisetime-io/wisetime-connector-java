/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.Lists;

import io.wisetime.generated.connect.TagMetadataDeleteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import io.wisetime.connector.metric.ApiClientMetricWrapper;
import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UnsubscribeRequest;
import io.wisetime.generated.connect.UpsertTagRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author yehor.lashkul
 */
class ApiClientMetricWrapperTest {

  private MetricService metricService;
  private ApiClient apiClient;
  private ApiClient apiClientMetricWrapper;

  @BeforeEach
  void setup() {
    metricService = mock(MetricService.class);
    apiClient = mock(ApiClient.class);
    apiClientMetricWrapper = new ApiClientMetricWrapper(apiClient, metricService);
  }

  @Test
  void tagUpsert_success() throws IOException {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    apiClientMetricWrapper.tagUpsert(upsertTagRequest);
    verify(metricService).increment(Metric.TAG_PROCESSED);
  }

  @Test
  void tagUpsert_error() throws IOException {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    doThrow(new RuntimeException("API ERROR"))
        .when(apiClient)
        .tagUpsert(any());

    assertThatThrownBy(() -> apiClientMetricWrapper.tagUpsert(upsertTagRequest))
        .as("Metric Wrapper shouldn't handle exceptions")
        .isInstanceOf(RuntimeException.class)
        .hasMessage("API ERROR");
    verify(metricService, never()).increment(Metric.TAG_PROCESSED);
  }

  @Test
  void tagUpsertBatch_success() throws IOException {
    ArrayList<UpsertTagRequest> upsertTagRequests = Lists.newArrayList(new UpsertTagRequest(), new UpsertTagRequest());
    apiClientMetricWrapper.tagUpsertBatch(upsertTagRequests);
    verify(metricService).increment(Metric.TAG_PROCESSED, 2);
  }

  @Test
  void tagUpsertBatch_error() throws IOException {
    ArrayList<UpsertTagRequest> upsertTagRequests = Lists.newArrayList(new UpsertTagRequest(), new UpsertTagRequest());
    doThrow(new RuntimeException("API ERROR"))
        .when(apiClient)
        .tagUpsertBatch(any());

    assertThatThrownBy(() -> apiClientMetricWrapper.tagUpsertBatch(upsertTagRequests))
        .as("Metric Wrapper shouldn't handle exceptions")
        .isInstanceOf(RuntimeException.class)
        .hasMessage("API ERROR");
    verify(metricService, never()).increment(eq(Metric.TAG_PROCESSED), anyInt());
  }
  
  @Test
  void otherMethods_delegateOnly() throws IOException {
    apiClientMetricWrapper.fetchTimeGroups(1);
    apiClientMetricWrapper.postedTimeSubscribe(new SubscribeRequest());
    apiClientMetricWrapper.tagAddKeywords(new AddKeywordsRequest());
    apiClientMetricWrapper.postedTimeUnsubscribe(new UnsubscribeRequest());
    apiClientMetricWrapper.tagAddKeywordsBatch(singletonList(new AddKeywordsRequest()));
    apiClientMetricWrapper.tagDelete(new DeleteTagRequest());
    apiClientMetricWrapper.tagDeleteKeyword(new DeleteKeywordRequest());
    apiClientMetricWrapper.teamInfo();
    apiClientMetricWrapper.updatePostedTimeStatus(new TimeGroupStatus());
    apiClientMetricWrapper.tagMetadataDelete(new TagMetadataDeleteRequest());
    verify(metricService, never()).increment(any());
    verify(metricService, never()).increment(any(), anyInt());
  }
}
