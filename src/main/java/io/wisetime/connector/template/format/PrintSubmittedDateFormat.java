/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Freemarker formatter to convert {@link io.wisetime.generated.connect.TimeRow#submittedDate} to human readable string.
 * To print hour:minutes use ${timeRow.getSubmittedDate()?string.@printSubmittedDate_HH\:mm}. You can set own date pattern.
 * See {@link DateTimeFormat} for pattern syntax.
 *
 * @author vadym
 */
public class PrintSubmittedDateFormat extends TemplateNumberFormat {

  private static final Logger log = LoggerFactory.getLogger(PrintSubmittedDateFormat.class);
  private static final DateTimeFormatter SUBMITTED_TIME_FOMATTER = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS");
  private final DateTimeFormatter outputFormatter;

  public PrintSubmittedDateFormat(String outputPattern) {
    this.outputFormatter = DateTimeFormat.forPattern(outputPattern);
  }

  @Override
  public String formatToPlainText(TemplateNumberModel numberModel) throws TemplateModelException {
    try {
      Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
      return outputFormatter.print(SUBMITTED_TIME_FOMATTER.parseDateTime(String.valueOf(number.longValue())));
    } catch (Exception e) {
      log.debug("Failed to format submitted date: {}", numberModel.getAsNumber());
      return "Unknown";
    }
  }

  @Override
  public boolean isLocaleBound() {
    return false;
  }

  @Override
  public String getDescription() {
    return "print submitted time to human readable string";
  }
}
