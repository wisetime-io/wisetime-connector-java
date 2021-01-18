/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

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
   * @param timeGroup the {@link TimeGroup} whose start time instant to calculate
   * @return start time instant of the time group
   */
  public static Optional<Instant> startInstant(final TimeGroup timeGroup) {
    return timeGroup
        .getTimeRows()
        .stream()
        .map(ActivityTimeCalculator::getFirstObservedTime)
        .sorted()
        .findFirst();
  }

  /**
   * This method is deprecated. Please use ActivityTimeCalculator#startInstant.
   *
   * The start time of a {@link TimeGroup} is the first observed time of the earliest {@link TimeRow} in the group.
   *
   * @param timeGroup the {@link TimeGroup} whose start time to calculate
   * @return start time in UTC
   */
  @Deprecated
  public static Optional<LocalDateTime> startTime(final TimeGroup timeGroup) {
    return startInstant(timeGroup)
        .map(i -> LocalDateTime.ofInstant(i, ZoneId.of("UTC")));
  }

  private static Instant getFirstObservedTime(final TimeRow timeRow) {
    return LocalDateTime.parse(
        timeRow.getActivityHour() + StringUtils.leftPad(timeRow.getFirstObservedInHour().toString(), 2, '0'),
        ACTIVITY_TIME_FORMATTER
    ).toInstant(ZoneOffset.UTC);
  }
}
