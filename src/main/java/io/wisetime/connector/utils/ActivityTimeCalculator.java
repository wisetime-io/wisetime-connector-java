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
        .map(ActivityTimeCalculator::getFirstObservedTime)
        .sorted()
        .findFirst();
  }

  /**
   * Returns the activity time of the time row in `yyyyMMddHHmm` format.
   */
  private static LocalDateTime getFirstObservedTime(final TimeRow timeRow) {
    return LocalDateTime.parse(
        timeRow.getActivityHour() + StringUtils.leftPad(timeRow.getFirstObservedInHour().toString(), 2, '0'),
        ACTIVITY_TIME_FORMATTER
    );
  }
}
