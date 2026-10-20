/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.Environment;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import java.util.Locale;

/**
 * Factory for duration freemarker extension {@link DurationNumberFormat}.
 *
 * @author vadym
 */
public class DurationNumberFormatFactory extends TemplateNumberFormatFactory {

  @Override
  public TemplateNumberFormat get(String params, Locale locale, Environment env) throws TemplateValueFormatException {
    TemplateFormatUtil.checkHasNoParameters(params);
    return new DurationNumberFormat();
  }
}
