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
   * The start time of a {@link TimeGroup} is the first observed time of the earliest {@link TimeRow} in the group.
   * <p>
   * Use this method in place of Optional<LocalDateTime> startTime(TimeGroup).
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

  private static Instant getFirstObservedTime(final TimeRow timeRow) {
    return LocalDateTime.parse(
        // e.g. 2018110209 & 3 -> 201811020903
        String.format("%s%02d", timeRow.getActivityHour(), timeRow.getFirstObservedInHour()),
        DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    ).toInstant(ZoneOffset.UTC);
  }
}
