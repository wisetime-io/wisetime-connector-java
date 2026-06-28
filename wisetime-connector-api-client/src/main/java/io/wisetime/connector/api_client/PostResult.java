/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Indicator of posted time status.
 *
 * @author thomas.haines
 * @author shane.xie@practiceinsight.io
 */
@SuppressWarnings({"MethodName", "AbbreviationAsWordInName"})
public class PostResult {

  private final PostResultStatus status;
  private String message;
  /**
   * Id in external system to reference entity created for this time group.
   */
  private String externalId;
  private Throwable error;

  /**
   * Indicates that posted time was processed correctly - no need for retry.
   */
  public static PostResult SUCCESS() {
    return new PostResult(PostResultStatus.SUCCESS);
  }

  /**
   * Indicates temporary error during processing posted time. WiseTime Connect will call the post time webhook again at a
   * later point to retry posting this time.
   */
  public static PostResult TRANSIENT_FAILURE() {
    return new PostResult(PostResultStatus.TRANSIENT_FAILURE);
  }

  /**
   * Posted time processing failed. Indicates that automatic retry is impossible.
   */
  public static PostResult PERMANENT_FAILURE() {
    return new PostResult(PostResultStatus.PERMANENT_FAILURE);
  }

  public static PostResult valueOf(String name) {
    return new PostResult(PostResultStatus.valueOf(name));
  }

  private PostResult(final PostResultStatus status) {
    this.status = status;
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
        status.getStatusCode() == PostResultStatus.TRANSIENT_FAILURE.getStatusCode()
            || status.getStatusCode() == PostResultStatus.PERMANENT_FAILURE.getStatusCode()
    );
    this.error = error;
    return this;
  }

  public PostResult withExternalId(final String externalId) {
    this.externalId = externalId;
    return this;
  }

  public PostResultStatus getStatus() {
    return status;
  }

  public String name() {
    return status.name();
  }

  public Optional<String> getMessage() {
    return Optional.ofNullable(message);
  }

  public Optional<Throwable> getError() {
    return Optional.ofNullable(error);
  }

  public Optional<String> getExternalId() {
    return Optional.ofNullable(externalId);
  }

  @Override
  public String toString() {
    MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
        .add("status", status)
        .add("message", message);
    if (error != null) {
      helper.add("error", ExceptionUtils.getStackTrace(error));
    }
    return helper.toString();
  }

  public enum PostResultStatus {
    /**
     * Indicates that posted time was processed correctly - no need for retry.
     */
    SUCCESS(200),
    /**
     * Indicates temporary error during processing posted time. WiseTime Connect will call the post time webhook again at a
     * later point to retry posting this time.
     */
    TRANSIENT_FAILURE(500),
    /**
     * Posted time processing failed. Indicates that automatic retry is impossible.
     */
    PERMANENT_FAILURE(400);

    private final int statusCode;

    PostResultStatus(final int statusCode) {
      this.statusCode = statusCode;
    }

    public int getStatusCode() {
      return statusCode;
    }
  }
}
