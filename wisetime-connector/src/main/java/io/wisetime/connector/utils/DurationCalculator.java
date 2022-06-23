/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.annotations.VisibleForTesting;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.util.function.Function;

/**
 * Utility to assist with calculating durations for a {@link TimeGroup}.
 *
 * @author shane.xie@practiceinsight.io
 */
public class DurationCalculator {

  private TimeGroup timeGroup;
  private DurationSource durationSource = DurationSource.TIME_GROUP;
  private boolean useExperienceWeighting = true;

  // default to 0.01 hour rounding
  private int roundToNearestSeconds = 36;

  /**
   * Create a DurationCalculator for a {@link TimeGroup}.
   * <p>
   * Usage example:
   * <pre>
   * DurationCalculator.of(timeGroup)
   *     .useDurationFrom(DurationSource.TIME_GROUP)
   *     .useExperienceWeighting();
   * </pre>
   *
   * @param timeGroup the TimeGroup whose durations we want to calculate
   * @return a DurationCalculator with the default configuration equivalent to:
   *
   *
   */
  public static DurationCalculator of(final TimeGroup timeGroup) {
    return new DurationCalculator(timeGroup);
  }

  private DurationCalculator(final TimeGroup timeGroup) {
    this.timeGroup = timeGroup;
  }

  /**
   * Tell the calculator whether to read the duration from the {@link TimeGroup} or its {@link TimeRow}s.
   *
   * @param durationSource which {@link DurationSource} option to use
   * @return the {@link DurationCalculator} with the duration from option applied
   */
  public DurationCalculator useDurationFrom(final DurationSource durationSource) {
    this.durationSource = durationSource;
    return this;
  }

  /**
   * Tell the calculator how many seconds to round to nearest value of.
   */
  public DurationCalculator roundToNearestSeconds(final int roundToNearestSeconds) {
    this.roundToNearestSeconds = roundToNearestSeconds;
    return this;
  }

  /**
   * Tell the calculator to use the user's experience weighting in its calculations.
   *
   * @return the {@link DurationCalculator} configured to use the user's experience weighting
   */
  public DurationCalculator useExperienceWeighting() {
    useExperienceWeighting = true;
    return this;
  }

  /**
   * Tell the calculator to use the user's experience weighting in its calculations.
   *
   * @return the {@link DurationCalculator} configured to use the user's experience weighting
   */
  @Deprecated
  public DurationCalculator useExperienceRating() {
    useExperienceWeighting();
    return this;
  }

  /**
   * Tell the calculator to disregard the user's experience weighting in its calculations.
   *
   * @return the {@link DurationCalculator} configured to ignore the user's experience weighting
   */
  public DurationCalculator disregardExperienceWeighting() {
    useExperienceWeighting = false;
    return this;
  }

  /**
   * Tell the calculator to disregard the user's experience weighting in its calculations.
   *
   * @return the {@link DurationCalculator} configured to ignore the user's experience weighting
   */
  @Deprecated
  public DurationCalculator disregardExperienceRating() {
    disregardExperienceWeighting();
    return this;
  }

  /**
   * Calculate durations for the {@link TimeGroup}
   *
   * @return Result containing the calculated per-tag and total durations
   */
  public long calculate() {
    final double totalDuration = sourceTotalDuration
        .andThen(applyExperienceWeighting)
        .apply(durationSource);

    long secondsValue = (long) Math.ceil(totalDuration);

    if (roundToNearestSeconds >= 2) {
      return getRoundedSecs(secondsValue);
    } else {
      return secondsValue;
    }
  }

  @VisibleForTesting
  long getRoundedSecs(Long rawSumSecs) {
    long roundModFromRaw = rawSumSecs % roundToNearestSeconds;
    if (roundModFromRaw > 0) {
      // round up to nearest `secondsRoundVal` number of seconds
      return (roundToNearestSeconds - roundModFromRaw) + rawSumSecs;
    } else {
      return rawSumSecs;
    }
  }

  /**
   * Determine base total duration, can be either of:
   * <p>
   * <ul>
   *   <li>TIME_GROUP: Total duration of the {@link TimeGroup}, in seconds</li>
   *   <li>SUM_TIME_ROWS: Sum of the duration for all {@link TimeRow}s in the {@link TimeGroup}, in seconds</li>
   * </ul>
   */
  private final Function<DurationSource, Double> sourceTotalDuration = durationSource -> {
    switch (durationSource) {
      case TIME_GROUP:
        return timeGroup.getTotalDurationSecs().doubleValue();
      case SUM_TIME_ROWS:
        return timeGroup.getTimeRows().stream().mapToDouble(TimeRow::getDurationSecs).sum();
      default:
        throw new IllegalStateException("Unhandled DurationSource option");
    }
  };

  /**
   * If {@link #useExperienceWeighting} is set to {@code}true{@code}, a percentage based on the user's experience weighting
   * is applied to the duration; otherwise return the full duration.
   */
  private final Function<Double, Double> applyExperienceWeighting = duration -> {
    if (useExperienceWeighting) {
      return duration * timeGroup.getUser().getExperienceWeightingPercent() / 100.0;
    }
    return duration;
  };
}
