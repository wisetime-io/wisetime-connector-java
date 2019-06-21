/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import io.wisetime.connector.config.info.ConnectorInfo;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import lombok.RequiredArgsConstructor;

/**
 * Simple {@link ObjectMapper} wrapper which is used to read and write JSON payload. Automatically adds {@link ConnectorInfo}
 * to payload if use any {@code writeWithInfo} method.
 *
 * @author yehor.lashkul
 */
@RequiredArgsConstructor
public class JsonPayloadService {

  public static final String CONNECTOR_INFO_KEY = "connectorInfo";

  private final ConnectorInfoProvider connectorInfoProvider;
  private final ObjectMapper mapper;

  /**
   * Deserializes JSON content from given JSON payload String.
   */
  public <T> T read(String payload, Class<T> valueType) throws IOException {
    return mapper.readValue(payload, valueType);
  }

  /**
   * Serializes any Java payload as a JSON String
   */
  public String write(Object payload) throws JsonProcessingException {
    return write(payload, false);
  }

  /**
   * Serializes any Java payload as a JSON String and adds {@link ConnectorInfo} to it
   */
  public String writeWithInfo(Object payload) throws JsonProcessingException {
    return write(payload, true);
  }

  /**
   * Serializes simple key-value pair as a JSON String and adds {@link ConnectorInfo} to it
   */
  public String writeWithInfo(String key, String value) throws JsonProcessingException {
    ObjectNode payload = mapper.createObjectNode().put(key, value);
    return writeWithInfo(payload);
  }

  private String write(Object payload, boolean withInfo) throws JsonProcessingException {
    return withInfo
        ? writeNodeWithInfo(mapper.valueToTree(payload))
        : mapper.writeValueAsString(payload);
  }

  private String writeNodeWithInfo(ObjectNode payload) throws JsonProcessingException {
    ConnectorInfo connectorInfo = connectorInfoProvider.get();
    JsonNode infoNode = mapper.valueToTree(connectorInfo);
    payload.set(CONNECTOR_INFO_KEY, infoNode);
    return mapper.writeValueAsString(payload);
  }
}
