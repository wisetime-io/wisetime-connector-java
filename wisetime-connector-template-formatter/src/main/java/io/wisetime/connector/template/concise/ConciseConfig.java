/*
 * Copyright (c) 2023 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.concise;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder(toBuilder = true)
public class ConciseConfig {

  @Builder.Default
  private int maxDescriptionLen = 255;

  @Builder.Default
  private String delimiter = ", ";

  @Builder.Default
  private String truncatedSuffix = "...";

}
