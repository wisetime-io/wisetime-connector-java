/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

/**
 * Whether to read duration from a TimeGroup or the sum of its TimeRow durations.
 *
 * @author shane.xie@practiceinsight.io
 */
public enum DurationSource {
  TIME_GROUP,
  SUM_TIME_ROWS
}
