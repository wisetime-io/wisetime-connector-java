/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template;

import com.google.common.base.Preconditions;

import freemarker.core.TemplateDateFormatFactory;
import freemarker.core.TemplateNumberFormatFactory;

import java.util.Map;
import java.util.TimeZone;

/**
 * Immutable configuration for {@link TemplateFormatter}. Already populated with default configuration.
 * <p>
 * If you need to change one value use:
 * <pre>
 * TemplateFormatterConfig.builder()
 *     .withWindownsClr(true)
 *     .build();
 * </pre>
 *
 * @author vadym
 */
public class TemplateFormatterConfig {

  /**
   * Default max length of user activity.
   * @see #getMaxLength()
   */
  public static final int DEFAULT_MAX_LENGTH = 0;
  /**
   * Default flag to use Windows compatible line delimiter.
   * @see #isUseWinclr()
   */
  public static final boolean DEFAULT_USE_WINCLR = false;
  /**
   * Default Freemarker template.
   * @see #getTemplatePath()
   */
  public static final String DEFAULT_TEMPLATE_PATH = "classpath:default-template.ftl";

  private final boolean useWinclr;
  private final int maxLength;
  private final String templatePath;
  private final TimeZone timezone;
  private final Map<String, TemplateDateFormatFactory> customDateFormats;
  private final Map<String, TemplateNumberFormatFactory> customNumberFormats;

  private TemplateFormatterConfig(Builder builder) {
    this.useWinclr = builder.winclr;
    this.maxLength = builder.maxLength;
    this.templatePath = builder.templatePath;
    this.timezone = builder.timezone;
    this.customDateFormats = builder.customDateFormats;
    this.customNumberFormats = builder.customNumberFormats;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static TemplateFormatterConfig defaultInstance() {
    return new Builder().build();
  }


  /**
   * @return whether or not use Windows-style CLR (\r\n) instead of \n.
   */
  public boolean isUseWinclr() {
    return useWinclr;
  }

  /**
   * @return 0 or negative value - no limit. Otherwise activity text return by {@link TemplateFormatter} guaranteed
   * to not exceed provided value (if longer - activity text will end with ...).
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * Path to Freemarker template. It can reference classpath or file system.
   * @see io.wisetime.connector.template.loader.TemplateLoaderHelperFactory
   */
  public String getTemplatePath() {
    return templatePath;
  }

  /**
   * The timezone to be used in templates. When a date formatter is used for a UTC date, it will convert the time to the
   * timezone specified.
   */
  public TimeZone getTimezone() {
    return timezone;
  }

  /**
   * Custom {@link TemplateDateFormatFactory} that can be used for formatting dates in template file.
   * @return
   */
  public Map<String, TemplateDateFormatFactory> getCustomDateFormats() {
    return customDateFormats;
  }

  /**
   * Custom {@link TemplateNumberFormatFactory} that can be used for formatting numbers in template file.
   * @return
   */
  public Map<String, TemplateNumberFormatFactory> getCustomNumberFormats() {
    return customNumberFormats;
  }

  /**
   * Builder for {@link TemplateFormatterConfig}. Recommended to use:
   * {@link TemplateFormatterConfig#builder()} to obtain instance.
   */
  public static class Builder {

    private boolean winclr = DEFAULT_USE_WINCLR;
    private int maxLength = DEFAULT_MAX_LENGTH;
    private String templatePath = DEFAULT_TEMPLATE_PATH;
    private TimeZone timezone;
    private Map<String, TemplateDateFormatFactory> customDateFormats;
    private Map<String, TemplateNumberFormatFactory> customNumberFormats;

    public Builder withWindowsClr(boolean useWinclr) {
      this.winclr = useWinclr;
      return this;
    }

    public Builder withMaxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    public Builder withTemplatePath(String templatePath) {
      this.templatePath = templatePath;
      return this;
    }

    public Builder withTimezone(TimeZone timezone) {
      this.timezone = timezone;
      return this;
    }

    public Builder withCustomDateFormats(Map<String, TemplateDateFormatFactory> customDateFormats) {
      this.customDateFormats = customDateFormats;
      return this;
    }

    public Builder withCustomNumberFormats(Map<String, TemplateNumberFormatFactory> customNumberFormats) {
      this.customNumberFormats = customNumberFormats;
      return this;
    }

    public TemplateFormatterConfig build() {
      Preconditions.checkNotNull(templatePath, "template path is required");
      return new TemplateFormatterConfig(this);
    }
  }
}
