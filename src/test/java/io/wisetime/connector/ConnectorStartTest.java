/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

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

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.metric.Metric;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.test_util.SparkTestUtil;
import io.wisetime.connector.test_util.TemporaryFolder;
import io.wisetime.connector.test_util.TemporaryFolderExtension;
import io.wisetime.connector.webhook.WebhookServerRunner;
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
public class ConnectorStartTest {
  private static final Logger log = LoggerFactory.getLogger(ConnectorStartTest.class);

  static TemporaryFolder testExtension;

  @Test
  void startAndQuery() throws Exception {
    log.info(testExtension.newFolder().getAbsolutePath());
    WiseTimeConnector mockConnector = mock(WiseTimeConnector.class);
    ApiClient mockApiClient = mock(ApiClient.class);
    MetricService metricService = mock(MetricService.class);

    Server server = createTestServer(mockConnector, mockApiClient, metricService);
    ObjectMapper objectMapper = TolerantObjectMapper.create();
    SparkTestUtil testUtil = new SparkTestUtil(getPort(server));

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
    verify(metricService).increment(Metric.TIME_GROUP_PROCESSED);
    clearInvocations(metricService);

    // PERMANENT_FAILURE
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.PERMANENT_FAILURE);
    SparkTestUtil.UrlResponse premanentFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(premanentFailureResponse.status).isEqualTo(400);
    verify(metricService, never()).increment(Metric.TIME_GROUP_PROCESSED);
    clearInvocations(metricService);

    // TRANSIENT_FAILURE
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.TRANSIENT_FAILURE);
    SparkTestUtil.UrlResponse transientFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(transientFailureResponse.status).isEqualTo(500);
    verify(metricService, never()).increment(Metric.TIME_GROUP_PROCESSED);
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
                                        ApiClient mockApiClient,
                                        MetricService metricService) throws Exception {
    System.setProperty(WEBHOOK_PORT.getConfigKey(), "0");
    ConnectorRunner runner = (ConnectorRunner) Connector.builder()
        .useWebhook()
        .withWiseTimeConnector(mockConnector)
        .withApiClient(mockApiClient)
        .withMetricService(metricService)
        .build();
    long startTime = System.currentTimeMillis();

    startServerRunner(runner);
    Server server = ((WebhookServerRunner) runner.getTimePosterRunner()).getServer();
    SparkTestUtil testUtil = new SparkTestUtil(getPort(server));

    RetryPolicy retryPolicy = new RetryPolicy()
        .retryOn(Exception.class)
        .withDelay(100, TimeUnit.MILLISECONDS)
        .withMaxRetries(40);

    Failsafe.with(retryPolicy).run(() -> getHome(testUtil));

    log.info((System.currentTimeMillis() - startTime) + "ms server start http://localhost:{}", getPort(server));
    return server;
  }

  /**
   * Start server runner for tests. Server start is not blocking, not timer tasks scheduled.
   */
  private static void startServerRunner(ConnectorRunner serverRunner) throws Exception {
    serverRunner.initWiseTimeConnector();
    serverRunner.getTimePosterRunner().start();
  }

  private static void getHome(SparkTestUtil testUtil) throws Exception {
    SparkTestUtil.UrlResponse response = testUtil.doMethod("GET", "/", null, "text/html");
    if (response.status != 200) {
      throw new IOException("Invalid response code: " + response.status);
    }
  }

  public static int getPort(Server server) {
    ServerConnector connector = (ServerConnector) server.getConnectors()[0];
    return connector.getLocalPort() <= 0 ? connector.getPort() : connector.getLocalPort();
  }
}
