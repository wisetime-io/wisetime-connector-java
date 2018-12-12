/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import java.util.function.Function;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

/**
 * Utility to assist with calculating durations for a TimeGroup.
 *
 * @author shane.xie@practiceinsight.io
 */
public class DurationCalculator {

  private TimeGroup timeGroup;
  private DurationFrom durationFrom = DurationFrom.TIME_GROUP;
  private boolean useExperienceRating = true;

  /**
   * Create a DurationCalculator for a TimeGroup.
   *
   * @param timeGroup the TimeGroup whose durations we want to calculate
   * @return a DurationCalculator with the default configuration equivalent to:
   *
   *   DurationCalculator.of(timeGroup)
   *       .useDurationFrom(DurationFrom.TIME_GROUP)
   *       .useExperienceRating()
   */
  public static DurationCalculator of(final TimeGroup timeGroup) {
    return new DurationCalculator(timeGroup);
  }

  private DurationCalculator(final TimeGroup timeGroup) {
    this.timeGroup = timeGroup;
  }

  /**
   * Tell the calculator whether to read the duration from the TimeGroup or its TimeRows.
   *
   * @param durationFrom which DurationFrom option to use
   * @return the DurationCalculator with the duration from option applied
   */
  public DurationCalculator useDurationFrom(final DurationFrom durationFrom) {
    this.durationFrom = durationFrom;
    return this;
  }

  /**
   * Tell the calculator to use the user's experience rating in its calculations.
   *
   * @return the DurationCalculator configured to use the user's experience rating
   */
  public DurationCalculator useExperienceRating() {
    useExperienceRating = true;
    return this;
  }

  /**
   * Tell the calculator to disregard the user's experience rating in its calculations.
   *
   * @return the DurationCalculator configured to ignore the user's experience rating
   */
  public DurationCalculator disregardExperienceRating() {
    useExperienceRating = false;
    return this;
  }

  /**
   * Calculate durations for the TimeGroup.
   *
   * @return Result containing the calculated per-tag and total durations
   */
  public Result calculate() {
    final double totalDuration = sourceTotalDuration
        .andThen(applyExperienceRating)
        .apply(durationFrom);

    final double perTagDuration = applyDurationSplitStrategy.apply(totalDuration);

    return new Result(Math.round(perTagDuration), Math.round(totalDuration));
  }

  public enum DurationFrom {
    TIME_GROUP,
    SUM_TIME_ROWS;
  }

  /**
   * The result of running DurationCalculator#calculate
   */
  public static class Result {

    private long perTagDuration;
    private long totalDuration;

    private Result(final long perTagDuration, final long totalDuration) {
      this.perTagDuration = perTagDuration;
      this.totalDuration = totalDuration;
    }

    /**
     * Get the calculated per-tag duration for the TimeGroup
     *
     * @return per-tag duration in seconds
     */
    public long getPerTagDuration() {
      return perTagDuration;
    }

    /**
     * Get the calculated total duration for the TimeGroup
     *
     * @return total duration in seconds
     */
    public long getTotalDuration() {
      return totalDuration;
    }
  }

  private final Function<DurationFrom, Double> sourceTotalDuration = durationFrom -> {
    switch (durationFrom) {
      case TIME_GROUP:
        return timeGroup.getTotalDurationSecs().doubleValue();
      case SUM_TIME_ROWS:
        return timeGroup.getTimeRows().stream().mapToDouble(TimeRow::getDurationSecs).sum();
      default:
        throw new IllegalStateException("Unhandled DurationFrom option");
    }
  };

  private final Function<Double, Double> applyExperienceRating = duration -> {
    if (useExperienceRating) {
      return duration * timeGroup.getUser().getExperienceWeightingPercent() / 100;
    }
    return duration;
  };

  private final Function<Double, Double> applyDurationSplitStrategy = duration -> {
    switch (timeGroup.getDurationSplitStrategy()) {
      case DIVIDE_BETWEEN_TAGS:
        return duration / timeGroup.getTags().size();
      case WHOLE_DURATION_TO_EACH_TAG:
        return duration;
      default:
        throw new IllegalStateException("Unhandled DurationSplitStrategy option");
    }
  };
}
