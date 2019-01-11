/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.generated.connect.TimeGroup;

/**
 * Utility to calculate Tag durations for a TimeGroup.
 *
 * @author shane.xie@practiceinsight.io
 *
 * @deprecated use {@link DurationCalculator} instead
 */
@Deprecated
public class TagDurationCalculator {

  /**
   * Calculate the duration to apply to each tag.
   * Takes into account the user's experience rating as well as the duration split strategy.
   *
   * @param timeGroup the TimeGroup for which to calculate each tag duration
   * @return duration, in seconds, to apply to each tag
   */
  public static double tagDuration(final TimeGroup timeGroup) {
    return tagDuration(timeGroup, false);
  }

  /**
   * Calculate the duration to apply to each tag.
   * Takes into account the TimeGroup's duration split strategy but disregards the user's experience rating.
   *
   * @param timeGroup the TimeGroup for which to calculate each tag duration
   * @return duration, in seconds, to apply to each tag
   */
  public static double tagDurationDisregardingExperienceRating(final TimeGroup timeGroup) {
    return tagDuration(timeGroup, true);
  }

  private static double tagDuration(final TimeGroup timeGroup, final boolean disregardExperienceRating) {
    if (timeGroup.getTags().size() == 0) {
      return 0;
    }

    double totalDuration = timeGroup.getTotalDurationSecs();

    if (!disregardExperienceRating) {
      totalDuration = timeGroup.getTotalDurationSecs() * timeGroup.getUser().getExperienceWeightingPercent() / 100.0;
    }

    switch (timeGroup.getDurationSplitStrategy()) {
      case WHOLE_DURATION_TO_EACH_TAG:
        return totalDuration;
      case DIVIDE_BETWEEN_TAGS:
      default:
        return totalDuration / timeGroup.getTags().size();
    }
  }
}
