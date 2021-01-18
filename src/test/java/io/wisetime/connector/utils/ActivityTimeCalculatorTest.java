/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static io.wisetime.connector.utils.ActivityTimeCalculator.startInstant;
import static io.wisetime.connector.utils.ActivityTimeCalculator.startTime;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie@practiceinsight.io
 */
class ActivityTimeCalculatorTest {

  private final FakeEntities fakeEntities = new FakeEntities();

  @Test
  void timeGroupStartInstant_no_time_rows() {
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(ImmutableList.of());

    assertThat(startInstant(timeGroup))
        .as("Can't calculate start time since there are no time rows")
        .isEmpty();
  }

  @Test
  void timeGroupStartInstant_same_hour_different_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(30);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startInstant(timeGroup))
            .as("should be the time of the time row with the earlier first observed in hour")
            .contains(Instant.parse("2018-11-02T09:03:00Z"));
  }

  @Test
  void timeGroupStartInstant_different_hour_same_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110211).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startInstant(timeGroup))
            .as("should be the time of the time row with the earlier activity hour")
            .contains(Instant.parse("2018-11-02T09:03:00Z"));
  }

  @Test
  void timeGroupStartTime_no_time_rows() {
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(ImmutableList.of());

    assertThat(startTime(timeGroup))
            .as("Can't calculate start time since there are no time rows")
            .isEmpty();
  }

  @Test
  void timeGroupStartTime_same_hour_different_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(30);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startTime(timeGroup))
        .as("should be the time of the time row with the earlier first observed in hour")
        .contains(LocalDateTime.of(2018, 11, 2, 9, 3));
  }

  @Test
  void timeGroupStartTime_different_hour_same_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110211).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(startTime(timeGroup))
        .as("should be the time of the time row with the earlier activity hour")
        .contains(LocalDateTime.of(2018, 11, 2, 9, 3));
  }
}
