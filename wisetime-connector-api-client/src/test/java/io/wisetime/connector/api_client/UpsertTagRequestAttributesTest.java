/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wisetime.connector.api_client.support.TolerantObjectMapper;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tag attributes are a free-form JSON object that replaces the whole attribute set on the server when sent, and
 * leaves the existing attributes unchanged when omitted.
 */
class UpsertTagRequestAttributesTest {

  private final ObjectMapper mapper = TolerantObjectMapper.create();

  @Test
  void attributes_areSerializedWithTheirJsonTypes() throws IOException {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("feeArrangementOptionIds", List.of("10", "11", "15"));
    attributes.put("category", "litigation");
    attributes.put("limit", 5);
    UpsertTagRequest request = new UpsertTagRequest().name("CASE01").attributes(attributes);

    JsonNode json = mapper.readTree(mapper.writeValueAsString(request));

    assertThat(json.path("attributes").path("feeArrangementOptionIds").isArray()).isTrue();
    assertThat(json.path("attributes").path("feeArrangementOptionIds").get(1).asText()).isEqualTo("11");
    assertThat(json.path("attributes").path("category").asText()).isEqualTo("litigation");
    assertThat(json.path("attributes").path("limit").isNumber()).isTrue();
  }

  @Test
  void attributes_emptySetIsSentToClearAttributes() throws IOException {
    UpsertTagRequest request = new UpsertTagRequest().name("CASE01").attributes(new HashMap<>());

    JsonNode json = mapper.readTree(mapper.writeValueAsString(request));

    assertThat(json.has("attributes")).isTrue();
    assertThat(json.get("attributes").size()).isZero();
  }

  @Test
  void attributes_nullIsOmitted() throws IOException {
    // The generated model defaults a free-form object to an empty map, so callers must set null explicitly
    // to leave the existing attributes unchanged.
    JsonNode json = mapper.readTree(mapper.writeValueAsString(new UpsertTagRequest().name("CASE01").attributes(null)));

    assertThat(json.hasNonNull("attributes")).isFalse();
  }

  @Test
  void attributes_notSetIsSentAsEmptyObject() throws IOException {
    // The generated model defaults a free-form object to an empty map, so an unset attributes field clears the
    // existing attributes on the server. Callers must set null explicitly to leave them unchanged.
    JsonNode json = mapper.readTree(mapper.writeValueAsString(new UpsertTagRequest().name("CASE01")));

    assertThat(json.has("attributes")).isTrue();
    assertThat(json.get("attributes").isObject()).isTrue();
    assertThat(json.get("attributes").size()).isZero();
  }

  @Test
  void attributes_roundTrip() throws IOException {
    UpsertTagRequest request = mapper.readValue(
        "{\"name\":\"CASE01\",\"attributes\":{\"category\":\"litigation\",\"ids\":[\"1\"]}}",
        UpsertTagRequest.class);

    assertThat(request.getAttributes()).containsEntry("category", "litigation").containsEntry("ids", List.of("1"));
  }
}
