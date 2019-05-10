/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.controller.ConnectorControllerImpl;
import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.test_util.SparkTestUtil;
import io.wisetime.connector.test_util.TemporaryFolder;
import io.wisetime.connector.test_util.TemporaryFolderExtension;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.User;

import static io.wisetime.connector.config.ConnectorConfigKey.WEBHOOK_PORT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author thomas.haines@practiceinsight.io
 */
@ExtendWith(TemporaryFolderExtension.class)
@SuppressWarnings("WeakerAccess")
public class WebhookTimePosterTest {
  private static final Logger log = LoggerFactory.getLogger(WebhookTimePosterTest.class);

  static TemporaryFolder testExtension;

  @Test
  void startAndQuery() throws Exception {
    RuntimeConfig.setProperty(WEBHOOK_PORT, "0");
    log.info(testExtension.newFolder().getAbsolutePath());
    WiseTimeConnector mockConnector = mock(WiseTimeConnector.class);
    MetricService metricService = mock(MetricService.class);

    Server server = createTestServer(mockConnector, metricService);
    ObjectMapper objectMapper = TolerantObjectMapper.create();
    SparkTestUtil testUtil = new SparkTestUtil(server.getURI().getPort());

    SparkTestUtil.UrlResponse pingResponse = testUtil.doMethod("GET", "/ping", null, "plain/text");
    assertThat(pingResponse.status).isEqualTo(200);

    assertThat(pingResponse.body)
        .as("expect pong response")
        .isEqualTo("pong");

    TimeGroup timeGroup = new TimeGroup()
        .user(new User())
        .tags(emptyList());
    String requestBody = objectMapper.writeValueAsString(timeGroup);

    // SUCCESS
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.SUCCESS);
    SparkTestUtil.UrlResponse sucessResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(sucessResponse.status).isEqualTo(200);
    clearInvocations(metricService);

    // PERMANENT_FAILURE
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.PERMANENT_FAILURE);
    SparkTestUtil.UrlResponse premanentFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(premanentFailureResponse.status).isEqualTo(400);
    clearInvocations(metricService);

    // TRANSIENT_FAILURE
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.TRANSIENT_FAILURE);
    SparkTestUtil.UrlResponse transientFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(transientFailureResponse.status).isEqualTo(500);
    clearInvocations(metricService);

    // METRIC
    MetricInfo metricInfo = MetricInfo.builder()
        .processedTags(1)
        .processedTimeGroups(2)
        .build();
    when(metricService.getMetrics()).thenReturn(metricInfo);
    SparkTestUtil.UrlResponse metricResponse = testUtil.doMethod("GET", "/metric", null, "plain/text");
    MetricInfo metricResponseBody = objectMapper.readValue(metricResponse.body, MetricInfo.class);
    assertThat(metricResponse.status).isEqualTo(200);
    assertThat(metricResponseBody).isEqualTo(metricInfo);

    if (System.getProperty("examine") != null) {
      server.join();
    } else {
      server.stop();
    }
  }

  public static Server createTestServer(WiseTimeConnector mockConnector,
                                        MetricService metricService) throws Exception {
    WebhookTimePoster webhookTimePoster = new WebhookTimePoster(0, mockConnector, metricService);
    webhookTimePoster.start();
    Server server = webhookTimePoster.getServer();
    SparkTestUtil testUtil = new SparkTestUtil(server.getURI().getPort());

    RetryPolicy retryPolicy = new RetryPolicy()
        .retryOn(Exception.class)
        .withDelay(100, TimeUnit.MILLISECONDS)
        .withMaxRetries(40);

    Failsafe.with(retryPolicy).run(() -> getHome(testUtil));
    return server;
  }

  private static void getHome(SparkTestUtil testUtil) throws Exception {
    SparkTestUtil.UrlResponse response = testUtil.doMethod("GET", "/", null, "text/html");
    if (response.status != 200) {
      throw new IOException("Invalid response code: " + response.status);
    }
  }

}
