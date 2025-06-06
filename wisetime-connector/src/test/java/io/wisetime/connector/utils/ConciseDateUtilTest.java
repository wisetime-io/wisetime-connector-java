/*
 * Copyright (c) 2023 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import io.wisetime.connector.test_util.FakeEntities;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConciseDateUtilTest {
  private final FakeEntities fakeEntities = new FakeEntities();

  private final ConciseDateUtil dateUtil = new ConciseDateUtil();

  @Test
  void findLocalDate_byTime() {
    final TimeRow row1 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(3);
    final TimeRow row2 = fakeEntities.randomTimeRow().activityHour(2018110209).firstObservedInHour(30);
    final TimeGroup timeGroup = fakeEntities.randomTimeGroup().timeRows(Lists.newArrayList(row1, row2));
    final ZoneOffset zoneOffset = ZoneOffset.ofHours(6);
    assertThat(dateUtil.findLocalDate(timeGroup, zoneOffset))
        .isEqualTo(LocalDate.parse("2018-11-02"));
  }

}
