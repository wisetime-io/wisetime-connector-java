/*
 * Copyright (c) 2023 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.generated.connect.TimeGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConciseDateUtil {

  /**
   * Method does not yet support static dates (for groups): falls back to earliest timeGroup#timeRow.
   *
   * @return LocalDate to use.
   */
  public LocalDate findLocalDate(TimeGroup timeGroup, ZoneOffset offsetZone) {
    Instant rowStartTime = ActivityTimeCalculator.startInstant(timeGroup).orElseGet(() -> {
      log.info("No start instance in time group rows");
      return null;
    });
    // Time when the user posted the time group. Measured in milliseconds since the Epoch.
    Instant submissionTime = null;
    if (timeGroup.getSubmissionTime() != null) {
      submissionTime = Instant.ofEpochMilli(timeGroup.getSubmissionTime());
    }

    final Optional<Instant> firstInstantOpt = Stream.of(rowStartTime, submissionTime)
        .filter(Objects::nonNull)
        .findFirst();

    if (firstInstantOpt.isEmpty()) {
      log.warn("No first instant available, returning now()");
      return ZonedDateTime.now(offsetZone).toLocalDate();
    }

    final Instant firstInstant = firstInstantOpt.get();
    return firstInstant.atOffset(offsetZone).toLocalDate();
  }

}
