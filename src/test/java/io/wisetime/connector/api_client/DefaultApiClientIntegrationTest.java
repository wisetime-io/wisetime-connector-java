/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.logging.DisabledMessagePublisher;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <pre>
 * To run, requires environment variable set:
 *   API_KEY="an-api-key-here"
 * </pre>
 *
 * @author thomas.haines@practiceinsight.io
 */
class DefaultApiClientIntegrationTest {
  private static final Logger log = LoggerFactory.getLogger(DefaultApiClientIntegrationTest.class);
  private static DefaultApiClient defaultApiClient;

  @BeforeAll
  static void setup() {
    Optional<String> apiKey = RuntimeConfig.getString(ConnectorConfigKey.API_KEY);
    boolean runnable = apiKey.isPresent()
        && !RuntimeConfig.getString(ConnectorConfigKey.API_BASE_URL).orElse("").contains("wisetime.io");
    if (!runnable) {
      log.info("DefaultApiClientIntegrationTest skipped");
      return;
    }
    RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey.get());
    defaultApiClient = new DefaultApiClient(requestExecutor, new DisabledMessagePublisher());
  }

  @Test
  void testTeamInfo() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    TeamInfoResult teamInfoResult = defaultApiClient.teamInfo();
    assertThat(teamInfoResult.getTeamName())
        .isNotBlank();
    log.info(teamInfoResult.toString());
  }

  @Test
  void tagUpsert() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("CreatedViaApi");
    request.setDescription("Tag from API");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("ViaCreatedApi"));
    defaultApiClient.tagUpsert(request);
  }

  @Test
  void tagUpsert_hasSpace() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("CreatedViaApi with space");
    request.setDescription("Tag from API");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("CreatedViaApi with space"));
    defaultApiClient.tagUpsert(request);
  }

  @Test
  void tagDelete() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagDelete("Management");
    defaultApiClient.tagDelete("Shared-1439");
  }

  @Test
  void tagAddKeywords() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagAddKeywords("CreatedViaApi", ImmutableSet.of("keyword_from_API", "keyword with space"));
  }

  @Test
  void tagDeleteKeyword() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagDeleteKeyword("CreatedViaApi", "keyword_from_API");
    defaultApiClient.tagDeleteKeyword("CreatedViaApi", "keyword with space");
  }

  @Test
  void postedTimeSubscribe() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    SubscribeRequest subscribeRequest = new SubscribeRequest();
    subscribeRequest.callbackUrl("http://testurl");
    subscribeRequest.setCallerKey("sample-caller-key");
    SubscribeResult response = defaultApiClient.postedTimeSubscribe(subscribeRequest);
    log.info(response.toString());
  }
}
