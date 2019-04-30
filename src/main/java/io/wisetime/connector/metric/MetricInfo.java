/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

import lombok.Data;

/**
 * @author yehor.lashkul
 */
@Data
class MetricInfo {
  private final int processedTags;
  private final int processedTimeGroups;
}
