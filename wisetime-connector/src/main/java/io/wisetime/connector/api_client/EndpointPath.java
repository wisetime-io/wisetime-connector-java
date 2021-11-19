/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.connector.api_client.support.ConnectApiRequest;

/**
 * Set of WiseTime API endpoints.
 */
public enum EndpointPath {

  TagDelete("/tag/delete", ConnectApiRequest.HttpMethod.POST),
  TagUpsert("/tag", ConnectApiRequest.HttpMethod.POST),

  /**
   * TagUpdateBatch
   */
  BulkTagUpsert("/tag/batch", ConnectApiRequest.HttpMethod.POST),

  TagAddKeyword("/tag/keyword", ConnectApiRequest.HttpMethod.POST),
  TagDeleteKeyword("/tag/keyword/delete", ConnectApiRequest.HttpMethod.POST),
  TagMetadataDelete("/tag/metadata/delete", ConnectApiRequest.HttpMethod.POST),

  ActivityTypesStartSyncSession("/activitytype/sync/start", ConnectApiRequest.HttpMethod.POST),
  ActivityTypesCompleteSyncSession("/activitytype/sync/complete", ConnectApiRequest.HttpMethod.POST),
  ActivityTypesCancelSyncSession("/activitytype/sync/cancel", ConnectApiRequest.HttpMethod.POST),
  BatchActivityTypesUpsert("/activitytype/batch", ConnectApiRequest.HttpMethod.POST),

  TeamInfo("/team/info", ConnectApiRequest.HttpMethod.GET),

  PostedTimeSubscribe("/postedtime/subscribe", ConnectApiRequest.HttpMethod.POST),
  PostedTimeUnsubscribe("/postedtime/unsubscribe", ConnectApiRequest.HttpMethod.POST),
  PostedTimeFetch("/postedtime?limit=:limit", ConnectApiRequest.HttpMethod.GET),
  PostedTimeUpdateStatus("/postedtime/status", ConnectApiRequest.HttpMethod.POST),

  TeamManagedConfig("/team/managed/config", ConnectApiRequest.HttpMethod.POST),

  HealthCheckFailureNotify("/healthcheck/failure/notify", ConnectApiRequest.HttpMethod.POST),
  HealthCheckFailureRescind("/healthcheck/failure/rescind", ConnectApiRequest.HttpMethod.POST);

  private final String actionPath;
  private ConnectApiRequest.HttpMethod httpMethod;

  EndpointPath(String actionPath, ConnectApiRequest.HttpMethod httpMethod) {
    this.actionPath = actionPath;
    this.httpMethod = httpMethod;
  }

  public String getActionPath() {
    return actionPath;
  }

  public ConnectApiRequest.HttpMethod getHttpMethod() {
    return httpMethod;
  }
}
