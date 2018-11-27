/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

/**
 * @author thomas.haines@practiceinsight.io
 */
public enum PostResult {

  SUCCESS(200),
  TRANSIENT_FAILURE(420),
  PERMANENT_FAILURE(428);

  private final int statusCode;

  PostResult(int statusCode) {
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
