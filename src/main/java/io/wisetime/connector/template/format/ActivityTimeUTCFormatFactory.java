/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.Environment;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;

import java.util.Locale;

/**
 * Factory for duration freemarker extension {@link ActivityTimeUTCFormat}.
 *
 * @author alvin.llobrera@practiceinsight.io
 */
public class ActivityTimeUTCFormatFactory extends TemplateNumberFormatFactory {

  @Override
  public TemplateNumberFormat get(String params, Locale locale, Environment env) throws TemplateValueFormatException {
    TemplateFormatUtil.checkHasNoParameters(params);
    return new ActivityTimeUTCFormat();
  }
}
