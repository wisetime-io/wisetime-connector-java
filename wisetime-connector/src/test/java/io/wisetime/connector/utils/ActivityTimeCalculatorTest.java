/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author shane.xie@practiceinsight.io
 */
class ActivityTimeCalculatorTest {

  private final FakeEntities fakeEntities = new FakeEntities();

  @Nested
  class TimeGroupStartInstant {
    @Test
    void noTimeRowsNoGroupInfo() {
      final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
          .timeRows(ImmutableList.of());

      assertThat(ActivityTimeCalculator.startInstant(timeGroup))
          .as("Can't calculate start time since there are no time rows and no group local date")
          .isEmpty();
    }

    @Test
    void noTimeRows() {
      final TimeGroup timeGroup = fakeEntities.randomTimeGroup()
          .timeRows(ImmutableList.of());

      LocalDate today = LocalDate.now();
      timeGroup.setLocalDate(today);
      timeGroup.setTzOffsetMins(8 * 60);

      long expectedEpochMillis = today.atStartOfDay(ZoneOffset.UTC)
          .plusHours(12)
          .minusMinutes(timeGroup.getTzOffsetMins()).toInstant().toEpochMilli();

      assertThat(ActivityTimeCalculator.startInstant(timeGroup))
          .as("calculate notional start time based on group-level params")
          .contains(Instant.ofEpochMilli(expectedEpochMillis));
    }
  }

  @Test
  void timeGroupStartInstant_same_hour_different_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(30);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(ActivityTimeCalculator.startInstant(timeGroup))
        .as("should be the time of the time row with the earlier first observed in hour")
        .contains(Instant.parse("2018-11-02T09:03:00Z"));
  }

  @Test
  void timeGroupStartInstant_different_hour_same_firstObservedInHour() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110211).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));

    assertThat(ActivityTimeCalculator.startInstant(timeGroup))
        .as("should be the time of the time row with the earlier activity hour")
        .contains(Instant.parse("2018-11-02T09:03:00Z"));
  }

}
