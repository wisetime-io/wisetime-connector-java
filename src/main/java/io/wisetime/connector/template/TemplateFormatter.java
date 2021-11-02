/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.wisetime.connector.template.TemplateFormatterConfig.DisplayZone;
import io.wisetime.connector.template.format.DurationNumberFormatFactory;
import io.wisetime.connector.template.format.PrintSubmittedDateFormatFactory;
import io.wisetime.connector.template.loader.TemplateLoaderHelper;
import io.wisetime.connector.template.loader.TemplateLoaderHelperFactory;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Template to format user activity text based on Freemarker engine.
 * <p>
 * Supports Windows CLR (see {@link TemplateFormatterConfig#isUseWinclr()}).
 * <p>
 * Supports max activity text length (see {@link TemplateFormatterConfig#getMaxLength()}).
 * <p>
 * Returned activity text is trimmed.
 *
 * @author vadym
 */
public class TemplateFormatter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final TemplateLoaderHelper templateLoaderHelper;
  private final TemplateFormatterConfig activityTextTemplateConfig;
  private final Configuration configuration;

  public TemplateFormatter(TemplateFormatterConfig activityTextTemplateConfig) {
    this.activityTextTemplateConfig = activityTextTemplateConfig;
    templateLoaderHelper = TemplateLoaderHelperFactory.from(activityTextTemplateConfig.getTemplatePath());
    configuration = createFreemarkerConfiguration(templateLoaderHelper.createTemplateLoader());
  }

  private Configuration createFreemarkerConfiguration(TemplateLoader templateLoader) {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
    configuration.setDefaultEncoding("UTF-8");
    configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    configuration.setLogTemplateExceptions(false);
    configuration.setWrapUncheckedExceptions(true);
    configuration.setTemplateLoader(templateLoader);
    configuration.setCustomNumberFormats(ImmutableMap.of(
        // this one provide support for duration format e.g.: ${durationSec?string.@duration}
        "duration", new DurationNumberFormatFactory(),
        // this one provide support for submitted time formatting e.g.:
        // ${timeRow.getSubmittedDate()?string.@printSubmittedDate_HH\:mm} to print with pattern HH:mm
        "printSubmittedDate", new PrintSubmittedDateFormatFactory()));

    return configuration;
  }

  /**
   * Format a TimeGroup into text that is suitable for output to integrated systems.
   * <p>
   * For example, we may want to generate text for inclusion into invoices that we'll send to clients.
   * <p>
   * This method throws a {@link TemplateProcessingException} if an error occurs when processing the template.
   *
   * @param timeGroup the TimeGroup to format
   * @return formatted text for the TimeGroup
   */
  public String format(TimeGroup timeGroup) {
    try {
      Template template = configuration.getTemplate(templateLoaderHelper.getTemplateName());
      StringWriter stringWriter = new StringWriter();
      template.process(convertToDisplayZone(timeGroup, activityTextTemplateConfig.getDisplayZone()), stringWriter);
      String result = stringWriter.toString().trim();
      if (activityTextTemplateConfig.isUseWinclr()) {
        result = result.replaceAll("\n", "\r\n");
      }
      if (needToCutString(result)) {
        result = result.substring(0, activityTextTemplateConfig.getMaxLength() - 3) + "...";
      }
      return result;
    } catch (IOException | TemplateException e) {
      throw new TemplateProcessingException("Failed to process template", e);
    }
  }

  @VisibleForTesting
  TimeGroup convertToDisplayZone(TimeGroup timeGroup, DisplayZone displayZone) {
    switch (displayZone) {
      case UTC:
        return timeGroup;
      case USER_LOCAL:
        return convertToUserLocal(timeGroup);
      default:
        throw new UnsupportedOperationException("Unsupported DisplayZone " + displayZone);
    }
  }

  private TimeGroup convertToUserLocal(TimeGroup timeGroupUtc) {
    try {
      // deep copy to/from json
      final String timeGroupUtcJson = OBJECT_MAPPER.writeValueAsString(timeGroupUtc);
      final TimeGroup timeGroupCopy = OBJECT_MAPPER.readValue(timeGroupUtcJson, TimeGroup.class);

      timeGroupCopy.getTimeRows().forEach(this::convertToUserLocal);
      return timeGroupCopy;
    } catch (IOException ex) {
      throw new RuntimeException("Failed to convert TimeGroup " + timeGroupUtc.getGroupId(), ex);
    }
  }

  private void convertToUserLocal(TimeRow timeRowUtc) {
    ZoneOffset rowTimeZone = ZoneOffset
        .ofHoursMinutes(timeRowUtc.getTimezoneOffsetMin() / 60, timeRowUtc.getTimezoneOffsetMin() % 60);
    DateTimeFormatter submittedTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    long submittedConverted = Long.parseLong(
        LocalDateTime.from(submittedTimeFormatter.parse(timeRowUtc.getSubmittedDate() + ""))
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(rowTimeZone)
            .format(submittedTimeFormatter));

    DateTimeFormatter segmentHourFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
    LocalDateTime localFirstObserved = LocalDateTime.from(segmentHourFormatter.parse(timeRowUtc.getActivityHour() + ""))
        .atZone(ZoneOffset.UTC)
        .plusMinutes(timeRowUtc.getFirstObservedInHour())
        .withZoneSameInstant(rowTimeZone)
        .toLocalDateTime();

    timeRowUtc
        .activityHour(Integer.valueOf(localFirstObserved.format(segmentHourFormatter)))
        .firstObservedInHour(localFirstObserved.getMinute())
        .setSubmittedDate(submittedConverted);
  }

  private boolean needToCutString(String result) {
    return activityTextTemplateConfig.getMaxLength() > 0 && result.length() > activityTextTemplateConfig.getMaxLength()
        && result.length() > 3;
  }
}
