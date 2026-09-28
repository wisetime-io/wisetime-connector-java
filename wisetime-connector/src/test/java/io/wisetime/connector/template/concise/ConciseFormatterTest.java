/*
 * Copyright (c) 2023 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.concise;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.javafaker.Faker;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConciseFormatterTest {
  private final Faker faker = Faker.instance();

  @Test
  void createDescription() {
    final ConciseFormatter formatter = new ConciseFormatter(
        ConciseConfig.builder()
            .maxDescriptionLen(35)
            .build());

    TimeGroup timeGroup = createTimeGroup();
    assertThat(formatter.createDescription(timeGroup))
        .as("rows should be collated and truncated")
        .isEqualTo("a-describe 1329879872341, b-desc...")
        .hasSize(35);
  }

  @Test
  void createDescription_group() {
    final ConciseFormatter formatter = new ConciseFormatter(
        ConciseConfig.builder()
            .build());

    TimeGroup timeGroup = createTimeGroup();
    timeGroup.setDescription("Quick brown fox jumped over lazy dog");
    assertThat(formatter.createDescription(timeGroup))
        .as("if group description exists, use that")
        .isEqualTo(timeGroup.getDescription());
  }

  private TimeGroup createTimeGroup() {
    TimeGroup timeGroup = new TimeGroup();
    TimeRow timeRow1 = new TimeRow();
    timeRow1.setDescription(faker.bothify("a-describe 1329879872341"));
    TimeRow timeRow2 = new TimeRow();
    timeRow2.setDescription(faker.bothify("b-describe 43287423987"));
    timeGroup.setTimeRows(List.of(timeRow1, timeRow2));
    return timeGroup;
  }

  @Test
  void combineAndTrim() {
    final int maxLen = 6;
    final ConciseFormatter formatter = new ConciseFormatter(ConciseConfig.builder()
        .maxDescriptionLen(maxLen)
        .truncatedSuffix("..")
        .build());

    assertThat(formatter.combineAndTrim(List.of("abcdefg")))
        .isEqualTo("abcd..")
        .hasSize(maxLen);

  }
}
