/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import java.util.function.Function;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

/**
 * Utility to assist with calculating durations for a {@link TimeGroup}.
 *
 * @author shane.xie@practiceinsight.io
 */
public class DurationCalculator {

  private TimeGroup timeGroup;
  private DurationSource durationSource = DurationSource.TIME_GROUP;
  private boolean useExperienceWeighting = true;

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
  public Result calculate() {
    final double totalDuration = sourceTotalDuration
        .andThen(applyExperienceWeighting)
        .apply(durationSource);

    return new Result(Math.round(totalDuration));
  }

  /**
   * The result of running {@link #calculate}
   */
  public static class Result {

    private long totalDuration;

    private Result(final long totalDuration) {
      this.totalDuration = totalDuration;
    }

    /**
     * Get the calculated per-tag duration for the {@link TimeGroup}
     *
     * For compatibility reasons with current usage of DurationCalculator returns same value as getTotalDuration
     *
     * @return per-tag duration in seconds
     */
    @Deprecated
    public long getPerTagDuration() {
      return totalDuration;
    }

    /**
     * Get the calculated total duration for the {@link TimeGroup}
     *
     * @return total duration in seconds
     */
    public long getTotalDuration() {
      return totalDuration;
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
