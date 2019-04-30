/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;

/**
 * @author yehor.lashkul
 */
@Data
@Builder
@JsonDeserialize(builder = MetricInfo.MetricInfoBuilder.class)
public class MetricInfo {
  private final int processedTags;
  private final int processedTimeGroups;

  @SuppressWarnings("WeakerAccess")
  @JsonPOJOBuilder(withPrefix = "")
  public static final class MetricInfoBuilder {

  }
}
