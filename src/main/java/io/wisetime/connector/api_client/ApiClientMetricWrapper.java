/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;
import java.util.List;

import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UpsertTagRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * @author yehor.lashkul
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

  @Override
  public void updatePostedTimeStatus(TimeGroupStatus timeGroupStatus) throws IOException {
    apiClient.updatePostedTimeStatus(timeGroupStatus);
    if (TimeGroupStatus.StatusEnum.SUCCESS.equals(timeGroupStatus.getStatus())) {
      metricService.increment(Metric.TIME_GROUP_PROCESSED);
    }
  }

  @SuppressWarnings("unused")
  private interface WithMetric {
    void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

    void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException;

    void updatePostedTimeStatus(TimeGroupStatus timeGroupStatus) throws IOException;
  }
}
