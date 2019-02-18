/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

/**
 * Utility to calculate activity times relevant to WiseTime.
 *
 * @author shane.xie@practiceinsight.io
 */
public class ActivityTimeCalculator {

  private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  /**
   * The start time of a {@link TimeGroup} is the first observed time of the earliest {@link TimeRow} in the group.
   *
   * @param timeGroup the {@link TimeGroup} whose start time to calculate
   * @return start time without timezone information
   */
  public static Optional<LocalDateTime> startTime(final TimeGroup timeGroup) {
    return timeGroup
        .getTimeRows()
        .stream()
        .map(ActivityTimeCalculator::getActivityTime)
        .min(String.CASE_INSENSITIVE_ORDER)
        .map(activityTime -> LocalDateTime.parse(activityTime, ACTIVITY_TIME_FORMATTER));
  }

  /**
   * Returns the activity time of the time row in `yyyyMMddHHmm` format.
   */
  private static String getActivityTime(final TimeRow timeRow) {
    return timeRow.getActivityHour() + StringUtils.leftPad(timeRow.getFirstObservedInHour().toString(), 2, '0');
  }
}
