/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import ch.qos.logback.core.PropertyDefinerBase;
import io.wisetime.connector.config.RuntimeConfig;

/**
 * Logback property definer.
 *
 * @author vadym
 */
public class RuntimeConfigPropertyDefiner extends PropertyDefinerBase {

  private String key;
  private String defaultValue = "";

  @Override
  public String getPropertyValue() {
    return RuntimeConfig.getString(() -> key).orElse(defaultValue);
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }
}
