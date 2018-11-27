/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

/**
 * @author thomas.haines@practiceinsight.io
 */
public enum ConnectorConfigKey implements RuntimeConfigKey {

  API_KEY("API_KEY"),
  PERSISTENT_DIR("PERSISTENT_DIR"),
  CONNECTOR_PROPERTIES_FILE("CONNECTOR_PROPERTIES_FILE"),
  HEALTH_MAX_MINS_SINCE_SUCCESS("HEALTH_MAX_MINS_SINCE_SUCCESS"),
  API_BASE_URL("API_BASE_URL");

  private final String configKey;

  ConnectorConfigKey(String configKey) {
    this.configKey = configKey;
  }

  @Override
  public String getConfigKey() {
    return configKey;
  }
}
