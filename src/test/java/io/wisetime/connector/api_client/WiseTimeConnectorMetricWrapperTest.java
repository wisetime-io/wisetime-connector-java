/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.github.javafaker.Faker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.metric.WiseTimeConnectorMetricWrapper;
import io.wisetime.generated.connect.TimeGroup;
import spark.Request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author vadym
 */
public class WiseTimeConnectorMetricWrapperTest {

  private static final Faker FAKER = new Faker();
  private MetricService metricService;
  private WiseTimeConnector connector;
  private WiseTimeConnector wiseTimeConnectorWrapper;

  @BeforeEach
  void setup() {
    metricService = mock(MetricService.class);
    connector = mock(WiseTimeConnector.class);
    wiseTimeConnectorWrapper = new WiseTimeConnectorMetricWrapper(connector, metricService);
  }

  @Test
  void postTime_success() {
    Request request = mock(Request.class);
    TimeGroup userPostedTime = new TimeGroup();
    when(connector.postTime(request, userPostedTime)).thenReturn(PostResult.SUCCESS());

    wiseTimeConnectorWrapper.postTime(request, userPostedTime);

    verify(metricService, times(1)).increment(Metric.TIME_GROUP_PROCESSED);
  }

  @Test
  void postTime_wrongCallerId() {
    Request request = mock(Request.class);
    TimeGroup userPostedTime = new TimeGroup();
    RuntimeConfig.setProperty(ConnectorConfigKey.CALLER_KEY, FAKER.lorem().word());
    assertThat(wiseTimeConnectorWrapper.postTime(request, userPostedTime))
        .usingRecursiveComparison()
        .as("caller id not matched")
        .isEqualTo(PostResult.PERMANENT_FAILURE().withMessage("Invalid caller key in posted time webhook call"));

    verifyZeroInteractions(connector);
    RuntimeConfig.clearProperty(ConnectorConfigKey.CALLER_KEY);
  }

  @Test
  void postTime_error() {
    Request request = mock(Request.class);
    TimeGroup userPostedTime = new TimeGroup();
    when(connector.postTime(request, userPostedTime)).thenReturn(PostResult.TRANSIENT_FAILURE());

    wiseTimeConnectorWrapper.postTime(request, userPostedTime);

    verify(metricService, never()).increment(Metric.TIME_GROUP_PROCESSED);
  }
}
