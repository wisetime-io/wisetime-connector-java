/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import io.wisetime.connector.testutils.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
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
        .isEqualTo(0)
        .as("Tag duration should be zero if there are no tags");
  }

  @Test
  void tagDuration_zero_experience_rating() {
    final User userWithNoExperience = fakeEntities.randomUser().experienceWeightingPercent(0);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().user(userWithNoExperience);

    assertThat(tagDuration(timeGroup))
        .isEqualTo(0)
        .as("Zero experience rating should result in zero duration to each tag");
  }

  @Test
  void tagDuration_divide_between_tags() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(105)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.DIVIDE_BETWEEN_TAGS);

    assertThat(tagDuration(timeGroup))
        .isEqualTo(10.5 / timeGroup.getTags().size())
        .as("Calculated duration should take into account experience rating and" +
            "split the total duration between the tags");
  }

  @Test
  void tagDuration_whole_duration_to_each_tag() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    assertThat(tagDuration(timeGroup))
        .isEqualTo(10)
        .as("Calculated duration should take into account experience rating and " +
            "assign whole total duration to each tag");
  }

  @Test
  void tagDurationDisregardingExperienceRating_should_disregard_experience_rating() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
        .user(user)
        .totalDurationSecs(100)
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    assertThat(tagDurationDisregardingExperienceRating(timeGroup))
        .isEqualTo(100)
        .as("Calculated duration should not take into account experience rating");
  }
}