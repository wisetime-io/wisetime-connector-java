/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import io.wisetime.connector.testutils.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import io.wisetime.generated.connect.User;

import static io.wisetime.connector.utils.TagDurationCalculator.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
class TagDurationCalculatorTest {

  private final FakeEntities fakeEntities = new FakeEntities();

  @Test
  void tagDuration_no_tags() {
    final TimeGroup timeGroupWithNoTags = fakeEntities.randomTimeGroup().tags(ImmutableList.of());

    assertThat(tagDuration(timeGroupWithNoTags))
        .as("Tag duration should be zero if there are no tags")
        .isEqualTo(0);
  }

  @Test
  void tagDuration_zero_experience_rating() {
    final User userWithNoExperience = fakeEntities.randomUser().experienceWeightingPercent(0);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().user(userWithNoExperience);

    assertThat(tagDuration(timeGroup))
        .as("Zero experience rating should result in zero duration to each tag")
        .isEqualTo(0);
  }

  @Test
  void tagDuration_divide_between_tags() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(105)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.DIVIDE_BETWEEN_TAGS);

    assertThat(tagDuration(timeGroup))
        .as("Calculated duration should take into account experience rating and" +
            "split the total duration between the tags")
        .isEqualTo(10.5 / timeGroup.getTags().size());
  }

  @Test
  void tagDuration_whole_duration_to_each_tag() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    assertThat(tagDuration(timeGroup))
        .as("Calculated duration should take into account experience rating and " +
            "assign whole total duration to each tag")
        .isEqualTo(10);
  }

  @Test
  void tagDurationDisregardingExperienceRating_should_disregard_experience_rating() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    assertThat(tagDurationDisregardingExperienceRating(timeGroup))
        .as("Calculated duration should not take into account experience rating")
        .isEqualTo(100);
  }

  @Test
  void tagDuration_should_use_time_group_duration() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    assertThat(tagDuration(timeGroup, true, true))
        .as("Calculated duration should use the defined time group duration")
        .isEqualTo(100);
  }

  @Test
  void tagDuration_should_use_time_rows_total_duration() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);
    timeGroup.getTimeRows().get(0).setDurationSecs(200);

    assertThat(tagDuration(timeGroup, false, true))
        .as("Calculated duration should use the actual total duration of all time rows in the group")
        .isEqualTo(timeGroup.getTimeRows().stream().mapToDouble(TimeRow::getDurationSecs).sum());
  }
}