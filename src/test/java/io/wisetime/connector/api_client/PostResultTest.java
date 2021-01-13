/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class PostResultTest {

  @Test
  void testToString_failure() {
    String failureMessage = "failureMessage";
    String exceptionMessage = "exceptionMessage";
    String causedBy = "causedBy";
    PostResult postResult = PostResult.TRANSIENT_FAILURE()
        .withMessage(failureMessage)
        .withError(new RuntimeException(exceptionMessage, new IllegalStateException(causedBy)));
    assertThat(postResult.toString())
        .as("check all key information preserved")
        .contains(PostResult.PostResultStatus.TRANSIENT_FAILURE.name(), failureMessage, exceptionMessage, causedBy);
  }

  @Test
  void testToString_success() {
    String statusMessage = "statusMessage";
    PostResult postResult = PostResult.SUCCESS()
        .withMessage(statusMessage);
    assertThat(postResult.toString())
        .as("check all key information preserved")
        .contains(PostResult.PostResultStatus.SUCCESS.name(), statusMessage);
  }
}
