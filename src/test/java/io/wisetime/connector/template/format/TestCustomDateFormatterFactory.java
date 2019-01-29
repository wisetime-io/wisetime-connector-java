/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.Environment;
import freemarker.core.TemplateDateFormat;
import freemarker.core.TemplateDateFormatFactory;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import freemarker.core.UnparsableValueException;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Custom TemplateDateFormat used for testing purposes
 *
 * @author alvin.llobrera@practiceinsight.io
 */
public class TestCustomDateFormatterFactory extends TemplateDateFormatFactory {


  @Override
  public TemplateDateFormat get(String params, int dateType, Locale locale, TimeZone timeZone,
                                boolean zonelessInput, Environment env) throws TemplateValueFormatException {
    return new TestCustomDateFormat();
  }

  private static class TestCustomDateFormat extends TemplateDateFormat {


    @Override
    public String formatToPlainText(TemplateDateModel dateModel) throws TemplateValueFormatException, TemplateModelException {
      return "The month is " + dateModel.getAsDate().toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDate()
          .getMonth();
    }

    @Override
    public Object parse(String s, int dateType) throws TemplateValueFormatException {
      try {
        return new Date(Long.parseLong(s));
      } catch (NumberFormatException e) {
        throw new UnparsableValueException("Malformed long");
      }
    }

    @Override
    public boolean isLocaleBound() {
      return false;
    }

    @Override
    public boolean isTimeZoneBound() {
      return false;
    }

    @Override
    public String getDescription() {
      return "Custom TemplateDateFormat used for testing purposes";
    }
  }
}
