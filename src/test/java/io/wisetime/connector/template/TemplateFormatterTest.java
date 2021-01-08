/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template;

import static org.assertj.core.api.Assertions.assertThat;

import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class TemplateFormatterTest {

  @Test
  public void format() throws Exception {
    TemplateFormatterConfig config = TemplateFormatterConfig.builder()
        .withTemplatePath("classpath:freemarker-template/test-template.ftl")
        .build();
    TemplateFormatter template = new TemplateFormatter(config);

    String result = template.format(prepareTimeGroup());

    assertThat(result)
        .as("check template formatter result")
        .isEqualTo("groupName [16m 40s]\ndescription");
  }

  private TimeGroup prepareTimeGroup() {
    TimeGroup timeGroup = new TimeGroup();
    timeGroup.setGroupName("groupName");
    timeGroup.setDescription("description");
    timeGroup.setTotalDurationSecs(1000);
    return timeGroup;
  }

  @Test
  public void format_winclr() throws Exception {
    TemplateFormatterConfig config = TemplateFormatterConfig.builder()
        .withTemplatePath("classpath:freemarker-template/test-template.ftl")
        .withWindowsClr(true)
        .build();
    TemplateFormatter template = new TemplateFormatter(config);

    String result = template.format(prepareTimeGroupWithTimeRow());

    assertThat(result)
        .as("check template formatter result")
        .isEqualTo("groupName [16m 40s]\r\ndescription\r\n\r\n  15:00 - Application - Window Title");
  }

  private TimeGroup prepareTimeGroupWithTimeRow() {
    TimeGroup timeGroup = prepareTimeGroup();
    timeGroup.setTimeRows(Collections.singletonList(prepareTimeRow()));
    return timeGroup;
  }

  private TimeRow prepareTimeRow() {
    TimeRow timeRow = new TimeRow();
    timeRow.setActivity("Application");
    timeRow.setDescription("Window Title");
    timeRow.setDurationSecs(200);
    timeRow.setSubmittedDate(20181110150000000L);
    return timeRow;
  }

  @Test
  public void format_tooLong() throws Exception {
    TemplateFormatterConfig config = TemplateFormatterConfig.builder()
        .withTemplatePath("classpath:freemarker-template/test-template.ftl")
        .withMaxLength(10)
        .build();
    TemplateFormatter template = new TemplateFormatter(config);

    String result = template.format(prepareTimeGroup());

    assertThat(result)
        .as("check template formatter result limited by 10 characters")
        .hasSize(10)
        .isEqualTo("groupNa...");
  }

}
