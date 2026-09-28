/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * Freemarker formatter to convert {@link io.wisetime.generated.connect.TimeRow#getSubmittedDate()} to human readable string.
 * To print hour:minutes use ${timeRow.getSubmittedDate()?string.@printSubmittedDate_HH\:mm}. You can set own date pattern.
 * See {@link DateTimeFormatter} for pattern syntax.
 *
 * @author vadym
 */
@Slf4j
public class PrintSubmittedDateFormat extends TemplateNumberFormat {

  private static final DateTimeFormatter SUBMITTED_TIME_FOMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
  private final DateTimeFormatter outputFormatter;

  public PrintSubmittedDateFormat(String outputPattern) {
    this.outputFormatter = DateTimeFormatter.ofPattern(outputPattern);
  }

  @Override
  public String formatToPlainText(TemplateNumberModel numberModel) throws TemplateModelException {
    try {
      Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
      return outputFormatter.format(SUBMITTED_TIME_FOMATTER.parse(String.valueOf(number.longValue())));
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
