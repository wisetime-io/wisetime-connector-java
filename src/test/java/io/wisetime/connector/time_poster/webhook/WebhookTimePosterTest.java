/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import static io.wisetime.connector.api_client.JsonPayloadService.CONNECTOR_INFO_KEY;
import static io.wisetime.connector.config.ConnectorConfigKey.WEBHOOK_PORT;
import static io.wisetime.connector.time_poster.webhook.WebhookApplication.MESSAGE_KEY;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.JsonPayloadService;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.config.info.ConnectorInfo;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.test_util.SparkTestUtil;
import io.wisetime.connector.test_util.TemporaryFolder;
import io.wisetime.connector.test_util.TemporaryFolderExtension;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.User;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author thomas.haines
 */
@ExtendWith(TemporaryFolderExtension.class)
@SuppressWarnings("WeakerAccess")
public class WebhookTimePosterTest {
  private static final Logger log = LoggerFactory.getLogger(WebhookTimePosterTest.class);
  private static final Faker FAKER = new Faker();
  static TemporaryFolder testExtension;

  @Test
  @SuppressWarnings({"checkstyle:methodlength", "checkstyle:executablestatementcount"})
  void startAndQuery() throws Exception {
    RuntimeConfig.setProperty(WEBHOOK_PORT, "0");
    String callerKey = FAKER.lorem().word();
    RuntimeConfig.setProperty(ConnectorConfigKey.CALLER_KEY, callerKey);
    log.info(testExtension.newFolder().getAbsolutePath());
    WiseTimeConnector mockConnector = mock(WiseTimeConnector.class);
    MetricService metricService = mock(MetricService.class);
    TimeGroupIdStore timeGroupIdStoreMock = mock(TimeGroupIdStore.class);
    ObjectMapper objectMapper = spy(TolerantObjectMapper.create());
    final ConnectorInfo connectorInfo = new ConnectorInfo("+08:00");
    JsonPayloadService payloadService = new JsonPayloadService(() -> connectorInfo, objectMapper);

    Server server = createTestServer(payloadService, mockConnector, metricService, timeGroupIdStoreMock);
    SparkTestUtil testUtil = new SparkTestUtil(server.getURI().getPort());

    // TODO(Dev) flaky test, should have retry/failsafe
    SparkTestUtil.UrlResponse pingResponse = testUtil.doMethod("GET", "/ping", null, "plain/text");
    assertThat(pingResponse.status).isEqualTo(200);

    assertThat(pingResponse.body)
        .as("expect pong response")
        .isEqualTo("pong");

    TimeGroup timeGroup = new TimeGroup()
        .user(new User())
        .callerKey(callerKey)
        .tags(emptyList());
    String requestBody = objectMapper.writeValueAsString(timeGroup);

    // SUCCESS
    when(timeGroupIdStoreMock.alreadySeenWebHook(any())).thenReturn(Optional.empty());
    when(mockConnector.postTime(any(), any())).thenReturn(PostResult.SUCCESS());
    SparkTestUtil.UrlResponse sucessResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(sucessResponse.status).isEqualTo(200);
    assertThat(sucessResponse.body).contains(CONNECTOR_INFO_KEY);
    verify(timeGroupIdStoreMock).putTimeGroupId(any(), eq(PostResultStatus.SUCCESS.name()), any());
    clearInvocations(metricService, timeGroupIdStoreMock, mockConnector);

    // SUCCESS deduplication
    when(timeGroupIdStoreMock.alreadySeenWebHook(any())).thenReturn(Optional.of(PostResult.SUCCESS()));
    SparkTestUtil.UrlResponse dedupSuccessResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(dedupSuccessResponse.status).isEqualTo(200);
    assertThat(dedupSuccessResponse.body).contains(CONNECTOR_INFO_KEY);
    verifyZeroInteractions(mockConnector);
    clearInvocations(metricService, timeGroupIdStoreMock);
    reset(timeGroupIdStoreMock);

    // PERMANENT_FAILURE
    // with failure message
    when(timeGroupIdStoreMock.alreadySeenWebHook(any())).thenReturn(Optional.empty());
    when(mockConnector.postTime(any(), any()))
        .thenReturn(PostResult.PERMANENT_FAILURE()
            .withMessage("Permanent failure message"));
    SparkTestUtil.UrlResponse permanentFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(permanentFailureResponse.status).isEqualTo(400);
    assertThat(permanentFailureResponse.body)
        .as("Body contains failure message from connector impl")
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"Permanent failure message\"");
    clearInvocations(metricService);

    // without failure message
    when(mockConnector.postTime(any(), any()))
        .thenReturn(PostResult.PERMANENT_FAILURE());
    SparkTestUtil.UrlResponse permanentFailureResponseWithoutMsg =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(permanentFailureResponseWithoutMsg.status).isEqualTo(400);
    assertThat(permanentFailureResponseWithoutMsg.body)
        .as("Body contains default failure message")
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"" + WebhookApplication.UNEXPECTED_ERROR + "\"");
    clearInvocations(metricService);

    // TRANSIENT_FAILURE
    // with failure message
    when(mockConnector.postTime(any(), any()))
        .thenReturn(PostResult.TRANSIENT_FAILURE()
            .withMessage("Transient failure message"));
    SparkTestUtil.UrlResponse transientFailureResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(transientFailureResponse.status).isEqualTo(500);
    assertThat(transientFailureResponse.body)
        .as("Body contains failure message from connector impl")
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"Transient failure message\"");
    clearInvocations(metricService);

    // without failure message
    when(mockConnector.postTime(any(), any()))
        .thenReturn(PostResult.TRANSIENT_FAILURE());
    SparkTestUtil.UrlResponse transientFailureResponseWithoutMsg =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(transientFailureResponseWithoutMsg.status).isEqualTo(500);
    assertThat(transientFailureResponseWithoutMsg.body)
        .as("Body contains default failure message")
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"" + WebhookApplication.UNEXPECTED_ERROR + "\"");
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

    // ERROR HANDLING
    // Unexpected error
    when(mockConnector.postTime(any(), any())).thenThrow(NullPointerException.class);
    SparkTestUtil.UrlResponse uncheckedExceptionResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(uncheckedExceptionResponse.status).isEqualTo(500);
    assertThat(uncheckedExceptionResponse.body)
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"" + WebhookApplication.UNEXPECTED_ERROR + "\"");
    clearInvocations(metricService);

    RuntimeConfig.setProperty(ConnectorConfigKey.CALLER_KEY, FAKER.lorem().word());
    when(timeGroupIdStoreMock.alreadySeenWebHook(any())).thenReturn(Optional.empty());
    SparkTestUtil.UrlResponse incorrectCallerIdResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(incorrectCallerIdResponse.status).isEqualTo(400);
    assertThat(incorrectCallerIdResponse.body)
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"Invalid caller key in posted time webhook call\"");
    RuntimeConfig.setProperty(ConnectorConfigKey.CALLER_KEY, callerKey);
    clearInvocations(metricService);

    // Invalid request
    when(objectMapper.readValue(requestBody, TimeGroup.class)).thenThrow(JsonParseException.class);
    SparkTestUtil.UrlResponse invalidRequestResponse =
        testUtil.doMethod("POST", "/receiveTimePostedEvent", requestBody, "application/json");
    assertThat(invalidRequestResponse.status).isEqualTo(400);
    assertThat(invalidRequestResponse.body)
        .contains(CONNECTOR_INFO_KEY)
        .contains("\"" + MESSAGE_KEY + "\":\"Invalid request\"");
    clearInvocations(metricService);
    RuntimeConfig.clearProperty(ConnectorConfigKey.CALLER_KEY);
    if (System.getProperty("examine") != null) {
      server.join();
    } else {
      server.stop();
    }
  }

  private static Server createTestServer(JsonPayloadService payloadService, WiseTimeConnector mockConnector,
                                        MetricService metricService,
                                        TimeGroupIdStore timeGroupIdStoreMock) throws Exception {
    WebhookTimePoster webhookTimePoster = new WebhookTimePoster(
        0,
        payloadService,
        mockConnector,
        metricService,
        timeGroupIdStoreMock
    );
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
