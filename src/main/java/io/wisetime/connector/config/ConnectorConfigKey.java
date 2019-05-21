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
   * Configured the method the connector should use to get posted time. Possible values:
   * - LONG_POLL: The connector will continuously try to fetch posted time and upload new tags to WiseTime
   * - WEBHOOK: The connector will act as server and passively listen for posted time and upload new tags to WiseTime
   * - TAG_ONLY: The connector will only upload new tags to WiseTime
   */
  CONNECTOR_MODE("CONNECTOR_MODE"),
  /**
   * The maximum amount of time groups to be fetched with each API call.
   * Will only be read when CONNECTOR_MODE is LONG_POLL
   */
  LONG_POLLING_LIMIT("LONG_POLLING_LIMIT"),
  /**
   * Number of parallel long polling threads. The threads are only used for the actual polling.
   * Interaction with the client system will happen in a single separate thread.
   * Defaults to two.
   * Will only be read when CONNECTOR_MODE is LONG_POLL
   */
  LONG_POLLING_THREADS("LONG_POLLING_THREADS"),
  /**
   * The caller key that WiseTime should provide with post time webhook calls. The connector does not authenticate webhook
   * calls if not set. Only relevant when CONNECTOR_MODE is WEBHOOK.
   */
  CALLER_KEY("CALLER_KEY"),
  /**
   * Path to internal WiseTime files. If not set - temp directory will be used.
   */
  DATA_DIR("DATA_DIR"),
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
   * Default port is 8080. Only relevant when CONNECTOR_MODE is WEBHOOK.
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
