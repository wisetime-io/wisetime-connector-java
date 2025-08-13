/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javafaker.Faker;
import io.wisetime.connector.api_client.EndpointPath;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
 */
class RestRequestExecutorTest {
  private String apiKey;
  private RestRequestExecutor restExec;

  @BeforeEach
  void setup() {
    apiKey = new Faker().numerify("apiKey######");
    restExec = new RestRequestExecutor(apiKey, "https://wisetime.test/connect/api");
  }

  @Test
  void addQueryParam() {
    assertThat(restExec.buildEndpointUri(EndpointPath.TagAddKeyword, Map.of("foo", "bar")).toString())
        .isEqualTo("https://wisetime.test/connect/api/tag/keyword?foo=bar");
  }

  @Test
  void mergeNamedParams_withSpace() {
    assertThat(restExec.buildEndpointUri(EndpointPath.TagAddKeyword, Map.of("foo", "bar baz")).toString())
        .isEqualTo("https://wisetime.test/connect/api/tag/keyword?foo=bar+baz");
  }

  @Test
  void mergeMulti() {
    assertThat(restExec.buildEndpointUri(EndpointPath.TagAddKeyword, Map.of("foo", "bar", "foot", "hand")).toString())
        .isIn("https://wisetime.test/connect/api/tag/keyword?foot=hand&foo=bar",
            "https://wisetime.test/connect/api/tag/keyword?foo=bar&foot=hand");
  }
}
