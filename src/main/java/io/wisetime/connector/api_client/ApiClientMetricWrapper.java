/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;
import java.util.List;

import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.UpsertTagRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Wrapper for {@link ApiClient} to gather statistics for uploaded tags.
 *
 * @author yehor.lashkul
 * @see Metric#TAG_PROCESSED
 */
@RequiredArgsConstructor
public class ApiClientMetricWrapper implements ApiClient {

  @Delegate(excludes = WithMetric.class)
  private final ApiClient apiClient;
  private final MetricService metricService;

  @Override
  public void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    apiClient.tagUpsert(upsertTagRequest);
    metricService.increment(Metric.TAG_PROCESSED);
  }

  @Override
  public void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException {
    apiClient.tagUpsertBatch(upsertTagRequests);
    metricService.increment(Metric.TAG_PROCESSED, upsertTagRequests.size());
  }

  @SuppressWarnings("unused")
  private interface WithMetric {
    void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

    void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException;
  }
}
