package io.wisetime.connector.template;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.TimeZone;

import io.wisetime.connector.template.format.TestCustomDateFormatterFactory;
import io.wisetime.connector.template.format.TestCustomNumberFormatterFactory;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  void format_with_custom_formats() {
    TimeGroup timeGroup = new TimeGroup()
        .groupName("1990-06-04T09:00:00Z") // using date as group name for testing
        .totalDurationSecs(64);
    TemplateFormatterConfig config = TemplateFormatterConfig.builder()
        .withTemplatePath("classpath:freemarker-template/test-template_with-custom-formats.ftl")
        .withCustomDateFormats(ImmutableMap.of("customDateFormat", new TestCustomDateFormatterFactory()))
        .withCustomNumberFormats(ImmutableMap.of("duration", new TestCustomNumberFormatterFactory()))
        .build();
    TemplateFormatter template = new TemplateFormatter(config);

    String result = template.format(timeGroup);

    assertThat(result)
        .as("custom formats should be used")
        .isEqualTo(
            "The month is JUNE\n" +
            "I got 64"
        );
  }

  @Test
  void format_check_timezone() {
    TimeGroup timeGroup = new TimeGroup()
        .groupName("1990-06-04T09:00:00Z") // using date as group name for testing
        .totalDurationSecs(64);
    TemplateFormatterConfig config = TemplateFormatterConfig.builder()
        .withTemplatePath("classpath:freemarker-template/test-template_time-zone.ftl")
        .withTimezone(TimeZone.getTimeZone("Asia/Manila"))
        .build();
    TemplateFormatter template = new TemplateFormatter(config);

    String result = template.format(timeGroup);

    assertThat(result)
        .as("custom formats should be used")
        .isEqualTo("Jun 4, 1990 5:00:00 PM");
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