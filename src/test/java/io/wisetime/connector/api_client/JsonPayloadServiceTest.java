/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.config.info.ConnectorInfo;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.connector.metric.MetricInfo;

import static io.wisetime.connector.api_client.JsonPayloadService.CONNECTOR_INFO_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author yehor.lashkul
 */
class JsonPayloadServiceTest {

  private final ObjectMapper mapper = TolerantObjectMapper.create();
  private final ConnectorInfo connectorInfo = new ConnectorInfo("+08:00");
  private final MetricInfo metricInfo = MetricInfo.builder().processedTags(1).processedTimeGroups(2).build();

  private JsonPayloadService jsonPayloadService;

  @BeforeEach
  void init() {
    ConnectorInfoProvider connectorInfoProviderMock = mock(ConnectorInfoProvider.class);
    jsonPayloadService = new JsonPayloadService(connectorInfoProviderMock, mapper);
    when(connectorInfoProviderMock.get()).thenReturn(connectorInfo);
  }

  @Test
  void testRead() throws Exception {
    String json = mapper.writeValueAsString(metricInfo);

    MetricInfo metricInfoPayload = jsonPayloadService.read(json, MetricInfo.class);

    assertThat(metricInfoPayload).isEqualTo(metricInfo);
  }

  @Test
  void testWrite() throws Exception {
    String resultPayload = jsonPayloadService.write(metricInfo);

    String payloadJson = "\"processedTags\":1,\"processedTimeGroups\":2";

    assertThat(resultPayload).contains(payloadJson);
    assertThat(resultPayload).doesNotContain(CONNECTOR_INFO_KEY);
  }

  @Test
  void testWriteWithInfo_keyValueString() throws Exception {
    String resultPayload = jsonPayloadService.writeWithInfo("message", "error message");

    String connectorInfoJson = String.format("\"%s\":%s", CONNECTOR_INFO_KEY, mapper.writeValueAsString(connectorInfo));
    String payloadJson = "\"message\":\"error message\"";

    assertThat(resultPayload).contains(connectorInfoJson, payloadJson);
  }

  @Test
  void testWriteWithInfo_pojo() throws Exception {
    String resultPayload = jsonPayloadService.writeWithInfo(metricInfo);

    String connectorInfoJson = String.format("\"%s\":%s", CONNECTOR_INFO_KEY, mapper.writeValueAsString(connectorInfo));
    String payloadJson = "\"processedTags\":1,\"processedTimeGroups\":2";

    assertThat(resultPayload).contains(connectorInfoJson, payloadJson);
  }
}
