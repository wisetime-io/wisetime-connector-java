/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.info.ConnectorInfo;
import io.wisetime.connector.utils.RuntimeEnvironmentUtil;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UnsubscribeRequest;
import io.wisetime.generated.connect.UnsubscribeResult;
import io.wisetime.generated.connect.UpsertTagRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * <pre>
 * To run, requires environment variable set:
 *   API_KEY="an-api-key-here"
 * </pre>
 *
 * @author thomas.haines
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
    defaultApiClient = new DefaultApiClient(apiKey.get());
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
  void tagUpsert_hasSlash() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("tag/name");
    request.setDescription("Tag from API with slash");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("key/word"));
    defaultApiClient.tagUpsert(request);
  }

  @Test
  void tagDelete() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagDelete(new DeleteTagRequest().name("CreatedViaApi"));
    defaultApiClient.tagDelete(new DeleteTagRequest().name("CreatedViaApi with space"));
    defaultApiClient.tagDelete(new DeleteTagRequest().name("tag/name"));
  }

  @Test
  void tagAddKeywords() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagAddKeywords(
        new AddKeywordsRequest()
            .tagName("CreatedViaApi")
            .additionalKeywords(ImmutableList.of("keyword_from_API", "keyword with space"))
    );
  }

  @Test
  void tagDeleteKeyword() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("CreatedViaApi").keyword("keyword_from_API"));
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("CreatedViaApi").keyword("keyword with space"));
  }

  @Test
  void tagAddKeywords_hasSlash() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagAddKeywords(
        new AddKeywordsRequest()
            .tagName("tag/name")
            .additionalKeywords(ImmutableList.of("key/word 1", "key/word 2"))
    );
  }

  @Test
  void tagDeleteKeyword_hasSlash() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("tag/name").keyword("key/word"));
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("tag/name").keyword("key/word 1"));
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

  @Test
  void postedTimeUnsubscribe() throws IOException {
    if (defaultApiClient == null) {
      return;
    }
    UnsubscribeRequest unsubscribeRequest = new UnsubscribeRequest();
    UnsubscribeResult response = defaultApiClient.postedTimeUnsubscribe(unsubscribeRequest);
    log.info(response.toString());
  }

  @Test
  void managedTimeConfig() throws IOException {
    if (defaultApiClient == null) {
      return;
    }

    ConnectorInfo connectorInfo = Mockito.mock(ConnectorInfo.class);
    when(connectorInfo.getClientTimeZoneOffset()).thenReturn(ZoneOffset.ofHours(3).getId());

    ManagedConfigRequest managedConfigRequest = new ManagedConfigRequest();
    managedConfigRequest.clientTimeZoneOffset(connectorInfo.getClientTimeZoneOffset());
    managedConfigRequest.setEnvironment(RuntimeEnvironmentUtil.getEnvProperties());
    managedConfigRequest.connectorType("test_connector_type");
    managedConfigRequest.setConnectorLibraryVersion(RuntimeEnvironmentUtil.getLibraryImplVersion());
    managedConfigRequest.clientTimestamp((double) Instant.now().getEpochSecond());
    ManagedConfigResponse configResponse = defaultApiClient.getTeamManagedConfig(managedConfigRequest);
    log.info(configResponse.toString());
  }
}
