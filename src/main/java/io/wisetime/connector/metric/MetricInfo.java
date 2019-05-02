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
  private final long processedTags;
  private final long processedTimeGroups;

  @SuppressWarnings("WeakerAccess")
  @JsonPOJOBuilder(withPrefix = "")
  public static final class MetricInfoBuilder {

  }
}
