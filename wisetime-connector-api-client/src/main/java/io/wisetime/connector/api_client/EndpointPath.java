/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.connector.api_client.support.ConnectApiRequest.HttpMethod;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Set of WiseTime API endpoints.
 */
@Getter
@RequiredArgsConstructor
public enum EndpointPath {

  TagDelete("tag/delete", HttpMethod.POST),
  TagUpsert("tag", HttpMethod.POST),

  /**
   * TagUpdateBatch
   */
  BulkTagUpsert("tag/batch", HttpMethod.POST),

  TagAddKeyword("tag/keyword", HttpMethod.POST),
  TagDeleteKeyword("tag/keyword/delete", HttpMethod.POST),
  TagMetadataDelete("tag/metadata/delete", HttpMethod.POST),

  BatchTagCategoryUpsert("tagcategory/batch", HttpMethod.POST),
  DeleteTagCategory("tagcategory/delete", HttpMethod.POST),

  ActivityTypesStartSyncSession("activitytype/sync/start", HttpMethod.POST),
  ActivityTypesCompleteSyncSession("activitytype/sync/complete", HttpMethod.POST),
  ActivityTypesCancelSyncSession("activitytype/sync/cancel", HttpMethod.POST),
  BatchActivityTypesUpsert("activitytype/batch", HttpMethod.POST),

  TeamInfo("team/info", HttpMethod.GET),

  PostedTimeSubscribe("postedtime/subscribe", HttpMethod.POST),
  PostedTimeUnsubscribe("postedtime/unsubscribe", HttpMethod.POST),
  PostedTimeFetch("postedtime", HttpMethod.GET, Set.of("limit")),
  PostedTimeUpdateStatus("postedtime/status", HttpMethod.POST),

  TeamManagedConfig("team/managed/config", HttpMethod.POST),

  HealthCheckFailureNotify("healthcheck/failure/notify", HttpMethod.POST),
  HealthCheckFailureRescind("healthcheck/failure/rescind", HttpMethod.POST);

  private final String actionPath;
  private final HttpMethod httpMethod;
  private final Set<String> requiredQueryParams;

  EndpointPath(String actionPath, HttpMethod httpMethod) {
    this(actionPath, httpMethod, Set.of());
  }
}
