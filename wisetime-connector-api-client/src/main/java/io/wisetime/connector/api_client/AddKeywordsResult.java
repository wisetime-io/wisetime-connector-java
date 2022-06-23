/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import lombok.Data;

/**
 * @author vadym
 */
@Data
public class AddKeywordsResult {

  private final String tagName;
  private final AddKeywordsStatus status;

  public enum AddKeywordsStatus {
    SUCCESS, TAG_NOT_FOUND
  }
}
