/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.connector.api_client.support.ConnectApiRequest;

/**
 * @author thomas.haines@practiceinsight.io
 */
public enum EndpointPath {

  TagDelete("/tag/:tagName/", ConnectApiRequest.HttpMethod.DELETE),
  TagUpsert("/tag/", ConnectApiRequest.HttpMethod.POST),
  TeamInfo("/team/info", ConnectApiRequest.HttpMethod.GET);

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
