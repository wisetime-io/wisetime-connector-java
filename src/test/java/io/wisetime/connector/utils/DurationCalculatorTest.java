/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import io.wisetime.generated.connect.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
class DurationCalculatorTest {

  private FakeEntities fakeEntities = new FakeEntities();

  @Test
  void config_defaultCalculator() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(50);

    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(120)
        .user(user);

    final DurationCalculator.Result result = DurationCalculator.of(timeGroup).calculate();

    assertThat(result.getTotalDuration())
        .isEqualTo(60)
        .as("Default calculator configuration should use the TimeGroup duration " +
            "and apply the user's experience weighting");
  }

  @Test
  void config_useDurationFrom_timeGroup() {
    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(120);

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .disregardExperienceWeighting()
        .useDurationFrom(DurationSource.TIME_GROUP)
        .calculate();

    assertThat(result.getTotalDuration())
        .isEqualTo(120)
        .as("Duration should be taken from the time group");
  }

  @Test
  void config_useDurationFrom_timeRows() {
    final TimeRow timeRow1 = fakeEntities.randomTimeRow().durationSecs(70);
    final TimeRow timeRow2 = fakeEntities.randomTimeRow().durationSecs(80);

    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(0)
        .timeRows(ImmutableList.of(timeRow1, timeRow2));

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .disregardExperienceWeighting()
        .useDurationFrom(DurationSource.SUM_TIME_ROWS)
        .calculate();

    assertThat(result.getTotalDuration())
        .isEqualTo(150)
        .as("Duration should be taken from the time rows");
  }

  @Test
  void config_useExperienceWeighting() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(10);

    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(105)
        .user(user);

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .useExperienceWeighting()
        .useDurationFrom(DurationSource.TIME_GROUP)
        .calculate();

    assertThat(result.getTotalDuration())
        .isEqualTo(11)
        .as("The user's experience weighting should be taken into account, and result rounded correctly");
  }

  @Test
  void config_disregardExperienceWeighting() {
    final User user = fakeEntities.randomUser().experienceWeightingPercent(50);

    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(120)
        .user(user);

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .disregardExperienceWeighting()
        .useDurationFrom(DurationSource.TIME_GROUP)
        .calculate();

    assertThat(result.getTotalDuration())
        .isEqualTo(120)
        .as("The user's experience weighting should be ignored");
  }

  @Test
  void calculate_durationSplitStrategy_divideBetweenTags() {
    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(200)
        .tags(ImmutableList.of(fakeEntities.randomTag(), fakeEntities.randomTag()))
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.DIVIDE_BETWEEN_TAGS);

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .disregardExperienceWeighting()
        .useDurationFrom(DurationSource.TIME_GROUP)
        .calculate();

    assertThat(result.getPerTagDuration())
        .isEqualTo(100)
        .as("Duration split between the two tags");
  }

  @Test
  void calculate_durationSplitStrategy_wholeDurationToEachTag() {
    final TimeGroup timeGroup = fakeEntities
        .randomTimeGroup()
        .totalDurationSecs(200)
        .tags(ImmutableList.of(fakeEntities.randomTag(), fakeEntities.randomTag()))
        .durationSplitStrategy(TimeGroup.DurationSplitStrategyEnum.WHOLE_DURATION_TO_EACH_TAG);

    final DurationCalculator.Result result = DurationCalculator
        .of(timeGroup)
        .disregardExperienceWeighting()
        .useDurationFrom(DurationSource.TIME_GROUP)
        .calculate();

    assertThat(result.getPerTagDuration())
        .isEqualTo(200)
        .as("Whole duration should be applied to each tags");
  }
}