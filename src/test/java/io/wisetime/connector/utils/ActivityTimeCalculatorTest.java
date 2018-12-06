/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import io.wisetime.connector.testutils.FakeEntities;
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
        .isEmpty()
        .as("Can't calculate start time since there are no time rows");
  }

  @Test
  void timeGroupStartTime_with_time_rows() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110110);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110109);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(ImmutableList.of(row1, row2));

    assertThat(startTime(timeGroup))
        .isEqualTo(Optional.of(LocalDateTime.of(2018, 11, 1, 9, 0)))
        .as("The start time is the segment hour of the earliest time row");
  }
}
