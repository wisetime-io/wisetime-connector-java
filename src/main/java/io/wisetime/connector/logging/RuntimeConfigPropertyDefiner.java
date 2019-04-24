/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import ch.qos.logback.core.PropertyDefinerBase;
import io.wisetime.connector.config.RuntimeConfig;

/**
 * Logback property definer.
 *
 * @author vadym
 */
public class RuntimeConfigPropertyDefiner extends PropertyDefinerBase {

  private String key;

  @Override
  public String getPropertyValue() {
    return RuntimeConfig.getString(() -> key).orElse("");
  }

  public void setKey(String key) {
    this.key = key;
  }
}
