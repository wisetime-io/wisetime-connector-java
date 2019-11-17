/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

import java.util.Optional;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.generated.connect.TimeGroup;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import spark.Request;

/**
 * Wrapper for {@link WiseTimeConnector} to gather statistics for posted time groups.
 *
 * @author vadym
 * @see Metric#TIME_GROUP_PROCESSED
 */
@RequiredArgsConstructor
public class WiseTimeConnectorMetricWrapper implements WiseTimeConnector {

  @Delegate(excludes = WithMetric.class)
  private final WiseTimeConnector wiseTimeConnector;
  private final MetricService metricService;

  @Override
  public PostResult postTime(Request request, TimeGroup userPostedTime) {
    Optional<String> callerKey = RuntimeConfig.getString(ConnectorConfigKey.CALLER_KEY);
    if (callerKey.isPresent() && !callerKey.get().equals(userPostedTime.getCallerKey())) {
      return PostResult.PERMANENT_FAILURE()
          .withMessage("Invalid caller key in posted time webhook call");
    }
    PostResult result = wiseTimeConnector.postTime(request, userPostedTime);
    if (result.getStatus() == PostResult.PostResultStatus.SUCCESS) {
      metricService.increment(Metric.TIME_GROUP_PROCESSED);
    }
    return result;
  }

  @SuppressWarnings("unused")
  private interface WithMetric {

    PostResult postTime(Request request, TimeGroup userPostedTime);
  }
}
