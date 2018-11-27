/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateValueFormatException;
import freemarker.core.UnformattableValueException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.utility.NumberUtil;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Formatter for freemarker to convert duration seconds to human readable normalized string.
 * E.g. 1000s will be converted to 16m 40s.
 * Sample usage in freemarker template: ${durationSec?string.@duration}
 *
 * @author vadym
 * @see DurationNumberFormatFactory
 */
public class DurationNumberFormat extends TemplateNumberFormat {

  private static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
      .appendHours().appendSuffix("h")
      .appendSeparator(" ")
      .appendMinutes().appendSuffix("m")
      .appendSeparator(" ")
      .appendSeconds().appendSuffix("s")
      .toFormatter();

  @Override
  public String formatToPlainText(TemplateNumberModel numberModel)
      throws TemplateValueFormatException, TemplateModelException {
    Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
    try {
      return PERIOD_FORMATTER.print(Period.seconds(NumberUtil.toIntExact(number)).normalizedStandard());
    } catch (ArithmeticException e) {
      throw new UnformattableValueException(number + " doesn't fit into an int");
    }
  }

  @Override
  public boolean isLocaleBound() {
    return false;
  }

  @Override
  public String getDescription() {
    return "format duration in seconds to human readable string e.g. 1000 seconds -> 16m 40s";
  }
}
