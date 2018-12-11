/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

/**
 * @author shane.xie@practiceinsight.io
 */
public class TagDurationCalculator {

  /**
   * Calculate the duration to apply to each tag.
   * Takes into account the user's experience rating as well as the duration split strategy.
   *
   * @param timeGroup the TimeGroup for which to calculate each tag duration
   * @return duration, in seconds, to apply to each tag
   */
  public static double tagDuration(final TimeGroup timeGroup) {
    return tagDuration(timeGroup, true, false);
  }

  /**
   * Calculate the duration to apply to each tag.
   * Takes into account the TimeGroup's duration split strategy but disregards the user's experience rating.
   *
   * @param timeGroup the TimeGroup for which to calculate each tag duration
   * @return duration, in seconds, to apply to each tag
   */
  public static double tagDurationDisregardingExperienceRating(final TimeGroup timeGroup) {
    return tagDuration(timeGroup, true, true);
  }


  /**
   * Calculate the tag duration to apply to each tag.
   *
   * @param timeGroup the TimeGroup for which to calculate each tag duration
   * @param useTimeGroupDuration if True, the method will use the time group's total duration to calculate tag duration,
   *                             otherwise, it will use the sum of the duration of each time row in the time group.
   * @param disregardExperienceRating if True, it will apply the user experience rating to calculate tag duration, otherwise,
   *                                  it will disregard user's experience rating.
   * @return duration, in seconds, to apply to each tag
   */
  public static double tagDuration(final TimeGroup timeGroup,
                                   final boolean useTimeGroupDuration,
                                   final boolean disregardExperienceRating) {
    if (timeGroup.getTags().size() == 0) {
      return 0;
    }

    double totalDuration = useTimeGroupDuration
        ? timeGroup.getTotalDurationSecs()
        : timeGroup.getTimeRows().stream().mapToDouble(TimeRow::getDurationSecs).sum();

    if (!disregardExperienceRating) {
      totalDuration = totalDuration * timeGroup.getUser().getExperienceWeightingPercent() / 100.0;
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
