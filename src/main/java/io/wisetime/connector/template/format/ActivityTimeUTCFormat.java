/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Freemarker formatter to convert the start activity time yyyyMMddHHmm to ISO-like date-time format with zone in
 * UTC (e.g. 2019-06-04T09:00:00Z).
 *
 * @see io.wisetime.generated.connect.TimeRow#activityHour
 * @see io.wisetime.generated.connect.TimeRow#firstObservedInHour
 *
 * @author alvin.llobrera@practiceinsight.io
 */
public class ActivityTimeUTCFormat extends TemplateNumberFormat {

  private static final Logger log = LoggerFactory.getLogger(PrintSubmittedDateFormat.class);
  private static final DateTimeFormatter ACTIVITY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  @Override
  public String formatToPlainText(TemplateNumberModel numberModel) throws TemplateModelException {
    try {
      Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
      return ZonedDateTime
          .of(LocalDateTime.parse(String.valueOf(number.longValue()), ACTIVITY_TIME_FORMATTER), ZoneOffset.UTC)
          .format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    } catch (Exception e) {
      log.debug("Failed to format activity time: {}", numberModel.getAsNumber());
      return "Unknown";
    }
  }

  @Override
  public boolean isLocaleBound() {
    return false;
  }

  @Override
  public String getDescription() {
    return "converts activity time (yyyyMMddHHmm) to ISO-like date-time format with offset and zone in UTC";
  }
}
