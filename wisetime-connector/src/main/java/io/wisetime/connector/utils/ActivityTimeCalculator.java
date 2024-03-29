/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Utility to calculate activity times relevant to WiseTime.
 *
 * @author shane.xie
 */
public class ActivityTimeCalculator {

  /**
   * <p>
   * The start time of a {@link TimeGroup} is the first observed time of the earliest {@link TimeRow} in the group. Use this
   * method in place of startTime(TimeGroup).
   * <p>
   *
   * @param timeGroup the {@link TimeGroup} whose start time instant to calculate
   * @return start time instant of the time group
   */
  public static Optional<Instant> startInstant(final TimeGroup timeGroup) {
    Optional<Instant> firstByRow = timeGroup
        .getTimeRows()
        .stream()
        .map(ActivityTimeCalculator::getFirstObservedTime)
        .sorted()
        .findFirst();

    if (firstByRow.isEmpty() && timeGroup.getLocalDate() != null) {
      // support for groups without time rows
      return Optional.of(Instant.ofEpochMilli(
          timeGroup.getLocalDate().atStartOfDay(ZoneOffset.UTC)
              // assign local date as UTC at midday
              .plusHours(12)
              // then adjust according to the user's TZ offset
              .minusMinutes(timeGroup.getTzOffsetMins()).toInstant().toEpochMilli()));
    }

    return firstByRow;
  }

  private static Instant getFirstObservedTime(final TimeRow timeRow) {
    return LocalDateTime.parse(timeRow.getActivityHour().toString(), DateTimeFormatter.ofPattern("yyyyMMddHH"))
        .plusMinutes(timeRow.getFirstObservedInHour())
        .toInstant(ZoneOffset.UTC);
  }
}
