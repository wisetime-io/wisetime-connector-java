/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import java.util.Optional;

/**
 * Immutable config object for use in writer/filter.
 */
class ConfigPojo {

  private String region;
  private String moduleName;
  private String logGroupName;
  private String propertiesFilePath;
  private boolean enabled = true;

  private int flushIntervalInSeconds = 4;

  int getFlushIntervalInSeconds() {
    return flushIntervalInSeconds;
  }

  Optional<String> getModuleName() {
    return Optional.ofNullable(moduleName);
  }

  Optional<String> getLogGroupName() {
    return Optional.ofNullable(logGroupName);
  }

  Optional<String> getPropertiesFilePath() {
    return Optional.ofNullable(propertiesFilePath);
  }

  Optional<String> getRegion() {
    return Optional.ofNullable(region);
  }

  boolean isEnabled() {
    return enabled;
  }

  @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
  static final class ConfigPojoBuilder {
    private int flushIntervalInSeconds = 3;
    private String moduleName;
    private String defaultLogGroup;
    private String region;

    private String propertiesFilePath;

    private boolean enabled = true;

    private ConfigPojoBuilder() {
    }

    public static ConfigPojoBuilder aConfigPojo() {
      return new ConfigPojoBuilder();
    }

    public ConfigPojoBuilder withFlushIntervalInSeconds(int flushIntervalInSeconds) {
      this.flushIntervalInSeconds = flushIntervalInSeconds;
      return this;
    }

    public ConfigPojoBuilder withDefaultLogGroup(String defaultLogGroup) {
      this.defaultLogGroup = defaultLogGroup;
      return this;
    }

    public ConfigPojoBuilder withModuleName(String moduleName) {
      this.moduleName = moduleName;
      return this;
    }

    public ConfigPojoBuilder withPropertiesFilePath(String propertiesFilePath) {
      this.propertiesFilePath = propertiesFilePath;
      return this;
    }

    public ConfigPojoBuilder withEnabled(String enabledStr) {
      // anything other than text 'false' assumes appender is enabled
      this.enabled = !"false".equalsIgnoreCase(enabledStr);
      return this;
    }

    public ConfigPojoBuilder withRegion(String region) {
      // anything other than text 'false' assumes appender is enabled
      this.region = region;
      return this;
    }


    public ConfigPojo build() {
      ConfigPojo configPojo = new ConfigPojo();
      configPojo.moduleName = this.moduleName;
      configPojo.logGroupName = this.defaultLogGroup;
      configPojo.flushIntervalInSeconds = this.flushIntervalInSeconds;
      configPojo.enabled = this.enabled;
      configPojo.propertiesFilePath = this.propertiesFilePath;
      configPojo.region = this.region;
      return configPojo;
    }
  }
}
