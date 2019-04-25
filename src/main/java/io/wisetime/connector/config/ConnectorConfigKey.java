/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

/**
 * List of environment configuration of WiseTime connector.
 *
 * @author thomas.haines@practiceinsight.io
 */
public enum ConnectorConfigKey implements RuntimeConfigKey {

  /**
   * More information about api key is here: <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
   */
  API_KEY("API_KEY"),
  /**
   * The caller key that WiseTime should provide with post time webhook calls. The connector does not authenticate webhook
   * calls if not set
   */
  CALLER_KEY("CALLER_KEY"),
  /**
   * Path to internal WiseTime files. If not set - temp directory will be used.
   */
  DATA_DIR("DATA_DIR"),
  /**
   * The token necessary for a shutdown command to work with the embedded Jetty server.
   */
  JETTY_SERVER_SHUTDOWN_TOKEN("JETTY_SERVER_SHUTDOWN_TOKEN"),
  /**
   * Properties file path (optional).
   */
  CONNECTOR_PROPERTIES_FILE("CONNECTOR_PROPERTIES_FILE"),
  /**
   * Timeout since last successful time posting. Used for health checks. Default is 60.
   */
  HEALTH_MAX_MINS_SINCE_SUCCESS("HEALTH_MAX_MINS_SINCE_SUCCESS"),
  /**
   * Default is https://wisetime.io/connect/api.
   */
  API_BASE_URL("API_BASE_URL"),
  /**
   * Default port is 8080
   */
  WEBHOOK_PORT("WEBHOOK_PORT"),

  /**
   * Level of root logger. Default value is INFO.
   */
  LOG_LEVEL("LOG_LEVEL");

  private final String configKey;

  ConnectorConfigKey(String configKey) {
    this.configKey = configKey;
  }

  @Override
  public String getConfigKey() {
    return configKey;
  }
}
