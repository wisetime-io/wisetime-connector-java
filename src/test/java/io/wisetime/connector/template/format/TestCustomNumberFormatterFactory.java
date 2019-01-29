/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.Environment;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.wisetime.connector.template.format.PrintSubmittedDateFormat;

/**
 * Custom TemplateNumberFormat used for testing purposes
 *
 * @author alvin.llobrera@practiceinsight.io
 */
public class TestCustomNumberFormatterFactory extends TemplateNumberFormatFactory {

  @Override
  public TemplateNumberFormat get(String params, Locale locale, Environment env) throws TemplateValueFormatException {
    TemplateFormatUtil.checkHasNoParameters(params);
    return new TestCustomNumberFormat();
  }

  private static class TestCustomNumberFormat extends TemplateNumberFormat {

    @Override
    public String formatToPlainText(TemplateNumberModel numberModel) {
      try {
        Number number = TemplateFormatUtil.getNonNullNumber(numberModel);
        return "I got " + number.longValue();
      } catch (Exception ex) {
        return "Unknown";
      }
    }

    @Override
    public boolean isLocaleBound() {
      return false;
    }

    @Override
    public String getDescription() {
      return "Custom TemplateNumberFormat used for testing purposes";
    }
  }
}
