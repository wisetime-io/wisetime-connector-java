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
   * Name of the local SQLite database file. If not set - default value will be used.
   */
  LOCAL_DB_FILENAME("LOCAL_DB_FILENAME"),
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
   * Flags if the core library runs as a stand-alone application or a sub-process.
   * Affects the HealthCheck. If true, it exits the environment on max successive failures reached.
   * Affects logging to the local DB. If true, no logging is saved in the local DB.
   */
  RUNNING_AS_MAIN_PROCESS("RUNNING_AS_MAIN_PROCESS"),
  /**
   * Default is https://wisetime.io/connect/api.
   */
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
