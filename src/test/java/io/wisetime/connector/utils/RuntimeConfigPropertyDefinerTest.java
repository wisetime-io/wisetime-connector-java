/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import org.junit.jupiter.api.Test;

import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.RuntimeConfigKey;
import io.wisetime.connector.utils.RuntimeConfigPropertyDefiner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yehor.lashkul
 */
class RuntimeConfigPropertyDefinerTest {

  private final String propertyKey = "test-key";
  private final RuntimeConfigKey configKey = () -> propertyKey;

  @Test
  void getPropertyValue_configExist() {
    RuntimeConfig.setProperty(configKey, "test-value");
    RuntimeConfigPropertyDefiner propertyDefiner = new RuntimeConfigPropertyDefiner();
    propertyDefiner.setKey(propertyKey);
    assertThat(propertyDefiner.getPropertyValue()).isEqualTo("test-value");
  }

  @Test
  void getPropertyValue_noConfigDefaultSet() {
    RuntimeConfig.clearProperty(configKey);
    RuntimeConfigPropertyDefiner propertyDefiner = new RuntimeConfigPropertyDefiner();
    propertyDefiner.setKey(propertyKey);
    propertyDefiner.setDefaultValue("test-default-value");
    assertThat(propertyDefiner.getPropertyValue()).isEqualTo("test-default-value");
  }

  @Test
  void getPropertyValue_noConfigNoDefault() {
    RuntimeConfig.clearProperty(configKey);
    RuntimeConfigPropertyDefiner propertyDefiner = new RuntimeConfigPropertyDefiner();
    propertyDefiner.setKey(propertyKey);
    assertThat(propertyDefiner.getPropertyValue()).isEqualTo("");
  }
}
