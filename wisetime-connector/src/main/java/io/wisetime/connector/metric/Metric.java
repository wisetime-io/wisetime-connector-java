/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

/**
 * @author yehor.lashkul
 */
public enum Metric {
  /**
   * Number of tags successfully uploaded by connector to WT servers.
   */
  TAG_PROCESSED,
  /**
   * Number of tags successfully saved to external system by connector.
   */
  TIME_GROUP_PROCESSED
}
