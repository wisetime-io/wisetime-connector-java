/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

import static io.wisetime.connector.utils.ActivityTimeCalculator.startTime;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
class ActivityTimeCalculatorTest {

  private final FakeEntities fakeEntities = new FakeEntities();

  @Test
  void timeGroupStartTime_no_time_rows() {
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(ImmutableList.of());

    assertThat(startTime(timeGroup))
        .as("Can't calculate start time since there are no time rows")
        .isEmpty();
  }

  @Test
  void timeGroupStartTime_different_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(30);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startTime(timeGroup))
        .as("should be the activity time of the earliest time row from the time group")
        .contains(LocalDateTime.of(2018, 11, 2, 9, 3));
  }

  @Test
  void timeGroupStartTime_same_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110211).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startTime(timeGroup))
        .as("should be the activity time of the earliest time row from the time group")
        .contains(LocalDateTime.of(2018, 11, 2, 9, 3));
  }
}
