/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.ImmutableList;
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
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.AddKeywordsResponse;
import io.wisetime.generated.connect.DeleteKeywordResponse;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

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
    Optional<String> apiKey = RuntimeConfig.findString(ConnectorConfigKey.API_KEY);
    boolean runnable = apiKey.isPresent()
        && !RuntimeConfig.findString(ConnectorConfigKey.API_BASE_URL).orElse("").contains("wisetime.io");
    if (!runnable) {
      log.info("DefaultApiClientIntegrationTest skipped");
      return;
    }
    RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey.get());
    defaultApiClient = new DefaultApiClient(requestExecutor);
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
    UpsertTagResponse response = defaultApiClient.tagUpsert(request);
    log.info("UpsertTagResponse={}", response.toString());
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
    UpsertTagResponse response = defaultApiClient.tagUpsert(request);
    log.info("UpsertTagResponse={}", response.toString());
  }

  @Test
  void tagDelete() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    DeleteTagResponse response = defaultApiClient.tagDelete("Management");
    log.info(response.toString());
    defaultApiClient.tagDelete("Shared-1439");
  }

  @Test
  void tagAddKeywords() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    AddKeywordsRequest request = new AddKeywordsRequest();
    request.setAdditionalKeywords(ImmutableList.of("keyword_from_API", "keyword with space"));
    AddKeywordsResponse response = defaultApiClient.tagAddKeywords("CreatedViaApi", request);
    log.info(response.toString());
  }

  @Test
  void tagDeleteKeyword() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    DeleteKeywordResponse response = defaultApiClient.tagDeleteKeyword("CreatedViaApi", "keyword_from_API");
    log.info(response.toString());

    response = defaultApiClient.tagDeleteKeyword("CreatedViaApi", "keyword with space");
    log.info(response.toString());
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
