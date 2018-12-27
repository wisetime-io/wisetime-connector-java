/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.base.Preconditions;

import java.util.Optional;

/**
 * Indicator of posted time status.
 *
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public enum PostResult {

  /**
   * Indicates that posted time was processed correctly - no need for retry.
   */
  SUCCESS(200),
  /**
   * Indicates temporary error during processing posted time. WiseTime Connect will call the post time webhook again at a
   * later point to retry posting this time.
   */
  TRANSIENT_FAILURE(420),
  /**
   * Posted time processing failed. Indicates that automatic retry is impossible.
   */
  PERMANENT_FAILURE(428);

  private final int statusCode;
  private String message;
  private Throwable error;

  PostResult(final int statusCode) {
    this.statusCode = statusCode;
  }

  /**
   * Provide a message with the post result
   *
   * @param message String describing the reason or cause behind the result
   * @return PostResult
   */
  public PostResult withMessage(final String message) {
    this.message = message;
    return this;
  }

  /**
   * Provide an error with the post result
   * Only applies to TRANSIENT_FAILURE or PERMANENT_FAILURE
   *
   * @param error Throwable the cause of post failure
   * @return PostResult
   */
  public PostResult withError(final Throwable error) {
    Preconditions.checkArgument(
        getStatusCode() == TRANSIENT_FAILURE.getStatusCode() || getStatusCode() == PERMANENT_FAILURE.getStatusCode()
    );
    this.error = error;
    return this;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Optional<String> getMessage() {
    return Optional.ofNullable(message);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }
}
