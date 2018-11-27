/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.Environment;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;

import java.util.Locale;

/**
 * Freemarker formatter factory for {@link PrintSubmittedDateFormat}.
 *
 * @author vadym
 */
public class PrintSubmittedDateFormatFactory extends TemplateNumberFormatFactory {

  @Override
  public TemplateNumberFormat get(String params, Locale locale, Environment env) {
    return new PrintSubmittedDateFormat(params);
  }
}
