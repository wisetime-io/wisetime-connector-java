/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template;

import com.google.common.collect.ImmutableMap;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;

import io.wisetime.connector.template.format.DurationNumberFormatFactory;
import io.wisetime.connector.template.format.PrintSubmittedDateFormatFactory;
import io.wisetime.connector.template.loader.TemplateLoaderHelper;
import io.wisetime.connector.template.loader.TemplateLoaderHelperFactory;
import io.wisetime.generated.connect.TimeGroup;


/**
 * Template to format user activity text based on Freemarker engine.
 * Supports Windows CLR (see {@link TemplateFormatterConfig#isUseWinclr()}).
 * Supports max activity text length (see {@link TemplateFormatterConfig#getMaxLength()}).
 * Returned activity text is trimmed.
 *
 * @author vadym
 */
public class TemplateFormatter {

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

  public String format(TimeGroup timeGroup) throws IOException, TemplateException {
    Template template = configuration.getTemplate(templateLoaderHelper.getTemplateName());
    StringWriter stringWriter = new StringWriter();
    template.process(timeGroup, stringWriter);
    String result = stringWriter.toString().trim();
    if (activityTextTemplateConfig.isUseWinclr()) {
      result = result.replaceAll("\n", "\r\n");
    }
    if (needToCutString(result)) {
      result = result.substring(0, activityTextTemplateConfig.getMaxLength() - 3) + "...";
    }
    return result;
  }

  private boolean needToCutString(String result) {
    return activityTextTemplateConfig.getMaxLength() > 0 && result.length() > activityTextTemplateConfig.getMaxLength()
        && result.length() > 3;
  }
}
