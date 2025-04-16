/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.wisetime.generated.connect.ActivityType;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import io.wisetime.generated.connect.SyncActivityTypesRequest;
import io.wisetime.generated.connect.SyncActivityTypesResponse;
import io.wisetime.generated.connect.SyncSession;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * To run, requires environment variable set:
 *   API_KEY="an-api-key-here"
 *   API_BASE_URL="api-base-url-here"
 * </pre>
 *
 * @author thomas.haines
 */
@RequiredArgsConstructor
class DefaultApiClientRunner {

  private static final Logger log = LoggerFactory.getLogger(DefaultApiClientRunner.class);
  private final DefaultApiClient defaultApiClient;

  public static void main(String[] args) throws Exception {
    String apiKey = Optional.ofNullable(System.getenv("API_KEY"))
        .orElseThrow(() -> new IllegalStateException("API_KEY is mandatory"));
    Preconditions.checkState(Optional.of(System.getenv("API_BASE_URL")).orElse("").contains("wisetime.com"),
        "api base url is required");
    final DefaultApiClientRunner runner = new DefaultApiClientRunner(new DefaultApiClient(apiKey));

    runner.testTeamInfo();
    runner.tagUpsert();
    runner.tagUpsert_hasSpace();
    runner.tagUpsert_hasSlash();
    runner.tagDelete();
    runner.tagAddKeywords();
    runner.tagDeleteKeyword();
    runner.tagAddKeywords_hasSlash();
    runner.tagDeleteKeyword_hasSlash();
    runner.activityTypesSyncSession();
    runner.syncActivityTypes_noSession();
    runner.managedTimeConfig();
  }

  void testTeamInfo() throws IOException {
    TeamInfoResult teamInfoResult = defaultApiClient.teamInfo();
    assertThat(teamInfoResult.getTeamName())
        .isNotBlank();
    log.info(teamInfoResult.toString());
  }

  void tagUpsert() throws IOException {
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("CreatedViaApi");
    request.setDescription("Tag from API");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("ViaCreatedApi"));
    defaultApiClient.tagUpsert(request);
  }

  void tagUpsert_hasSpace() throws IOException {
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("CreatedViaApi with space");
    request.setDescription("Tag from API");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("CreatedViaApi with space"));
    defaultApiClient.tagUpsert(request);
  }

  void tagUpsert_hasSlash() throws IOException {
    UpsertTagRequest request = new UpsertTagRequest();
    request.setName("tag/name");
    request.setDescription("Tag from API with slash");
    request.setPath("/");
    request.setAdditionalKeywords(Lists.newArrayList("key/word"));
    defaultApiClient.tagUpsert(request);
  }

  void tagDelete() throws IOException {
    defaultApiClient.tagDelete(new DeleteTagRequest().name("CreatedViaApi"));
    defaultApiClient.tagDelete(new DeleteTagRequest().name("CreatedViaApi with space"));
    defaultApiClient.tagDelete(new DeleteTagRequest().name("tag/name"));
  }

  void tagAddKeywords() throws IOException {
    defaultApiClient.tagAddKeywords(
        new AddKeywordsRequest()
            .tagName("CreatedViaApi")
            .additionalKeywords(ImmutableList.of("keyword_from_API", "keyword with space"))
    );
  }

  void tagDeleteKeyword() throws IOException {
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("CreatedViaApi").keyword("keyword_from_API"));
    defaultApiClient.tagDeleteKeyword(
        new DeleteKeywordRequest().tagName("CreatedViaApi").keyword("keyword with space"));
  }

  void tagAddKeywords_hasSlash() throws IOException {
    defaultApiClient.tagAddKeywords(
        new AddKeywordsRequest()
            .tagName("tag/name")
            .additionalKeywords(ImmutableList.of("key/word 1", "key/word 2"))
    );
  }

  void tagDeleteKeyword_hasSlash() throws IOException {
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("tag/name").keyword("key/word"));
    defaultApiClient.tagDeleteKeyword(new DeleteKeywordRequest().tagName("tag/name").keyword("key/word 1"));
  }

  void activityTypesSyncSession() throws IOException {
    final SyncSession syncSession = defaultApiClient.activityTypesStartSyncSession();
    assertThat(syncSession.getSyncSessionId()).isNotBlank();
    defaultApiClient.activityTypesCancelSyncSession(syncSession);

    final SyncSession syncSession2 = defaultApiClient.activityTypesStartSyncSession();
    defaultApiClient.syncActivityTypes(new SyncActivityTypesRequest()
        .activityTypes(ImmutableList.of(new ActivityType()
            .code("CODE-api-test")
            .label("LABEL-api-test")
            .description("DESCRIPTION-api-test")))
        .syncSessionId(syncSession2.getSyncSessionId()));
    defaultApiClient.activityTypesCompleteSyncSession(syncSession2);
  }

  void syncActivityTypes_noSession() throws IOException {
    final SyncActivityTypesResponse response = defaultApiClient
        .syncActivityTypes(new SyncActivityTypesRequest()
            .activityTypes(ImmutableList.of(new ActivityType()
                .code("CODE-api-test")
                .label("LABEL-api-test")
                .description("DESCRIPTION-api-test"))));

    assertThat(response.getErrors())
        .isNullOrEmpty();
  }

  void managedTimeConfig() throws IOException {
    final Map<String, String> envProps = new HashMap<>();
    envProps.put("java_vm_specific_version", System.getProperty("java.vm.specification.version"));
    envProps.put("java_vm_version", System.getProperty("java.vm.version"));
    envProps.put("java_vm_name", System.getProperty("java.vm.name"));
    envProps.put("client_os", System.getProperty("os.name"));

    ManagedConfigRequest managedConfigRequest = new ManagedConfigRequest();
    managedConfigRequest.clientTimeZoneOffset(ZoneOffset.ofHours(3).getId());
    managedConfigRequest.setEnvironment(envProps);
    managedConfigRequest.connectorType("test_connector_type");
    managedConfigRequest.setConnectorLibraryVersion("1.0.0");
    managedConfigRequest.clientTimestamp(Instant.now().getEpochSecond());
    ManagedConfigResponse configResponse = defaultApiClient.getTeamManagedConfig(managedConfigRequest);
    log.info(configResponse.toString());
  }
}
