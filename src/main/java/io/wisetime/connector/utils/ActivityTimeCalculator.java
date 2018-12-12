/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

/**
 * Utility to calculate activity times relevant to WiseTime.
 *
 * @author shane.xie@practiceinsight.io
 */
public class ActivityTimeCalculator {

  private static final DateTimeFormatter ACTIVITY_HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

  /**
   * The start time of a TimeGroup is the start of the segment hour of the earliest TimeRow in the group.
   *
   * @param timeGroup the TimeGroup whose start time to calculate
   * @return start time without timezone information
   */
  public static Optional<LocalDateTime> startTime(final TimeGroup timeGroup) {
    return timeGroup
        .getTimeRows()
        .stream()
        .min(Comparator.comparingInt(TimeRow::getActivityHour))
        .map(TimeRow::getActivityHour)
        .map(hour -> LocalDateTime.parse(String.valueOf(hour), ACTIVITY_HOUR_FORMATTER));
  }
}
