/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.connector.api_client.support.ConnectApiRequest.HttpMethod;

/**
 * Set of WiseTime API endpoints.
 */
public enum EndpointPath {

  TagDelete("/tag/delete", HttpMethod.POST),
  TagUpsert("/tag", HttpMethod.POST),

  /**
   * TagUpdateBatch
   */
  BulkTagUpsert("/tag/batch", HttpMethod.POST),

  TagAddKeyword("/tag/keyword", HttpMethod.POST),
  TagDeleteKeyword("/tag/keyword/delete", HttpMethod.POST),
  TagMetadataDelete("/tag/metadata/delete", HttpMethod.POST),

  TagCategoryFind("/tagcategory/find", HttpMethod.GET),
  TagCategoryCreate("/tagcategory/create", HttpMethod.POST),
  TagCategoryUpdate("/tagcategory/update", HttpMethod.POST),

  ActivityTypesStartSyncSession("/activitytype/sync/start", HttpMethod.POST),
  ActivityTypesCompleteSyncSession("/activitytype/sync/complete", HttpMethod.POST),
  ActivityTypesCancelSyncSession("/activitytype/sync/cancel", HttpMethod.POST),
  BatchActivityTypesUpsert("/activitytype/batch", HttpMethod.POST),

  TeamInfo("/team/info", HttpMethod.GET),

  PostedTimeSubscribe("/postedtime/subscribe", HttpMethod.POST),
  PostedTimeUnsubscribe("/postedtime/unsubscribe", HttpMethod.POST),
  PostedTimeFetch("/postedtime?limit=:limit", HttpMethod.GET),
  PostedTimeUpdateStatus("/postedtime/status", HttpMethod.POST),

  TeamManagedConfig("/team/managed/config", HttpMethod.POST),

  HealthCheckFailureNotify("/healthcheck/failure/notify", HttpMethod.POST),
  HealthCheckFailureRescind("/healthcheck/failure/rescind", HttpMethod.POST);

  private final String actionPath;
  private final HttpMethod httpMethod;

  EndpointPath(String actionPath, HttpMethod httpMethod) {
    this.actionPath = actionPath;
    this.httpMethod = httpMethod;
  }

  public String getActionPath() {
    return actionPath;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }
}
