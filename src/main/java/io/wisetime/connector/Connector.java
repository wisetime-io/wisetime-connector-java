package io.wisetime.connector;

import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.integrate.WiseTimeConnector;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point of WiseTime connector. Instance of service is created by {@link ConnectorBuilder}:
 * <pre>
 *     ConnectorController controller = Connector.builder()
 *         .useWebhook()
 *         .withWiseTimeConnector(myConnector)
 *         .withApiKey(apiKey)
 *         .build();
 * </pre>
 *
 * You can then start the connector by calling {@link ConnectorController#start()}. This is a blocking call, meaning that the
 * current thread will wait till the application dies.
 * <p>
 * Main extension point is {@link WiseTimeConnector}.
 * <p>
 * More information regarding the API key can be found here:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 *
 * @author yehor.lashkul
 */
@Slf4j
@SuppressWarnings("WeakerAccess")
public abstract class Connector {
  /**
   * Method to create new {@link ConnectorBuilder} instance. Automatically checks system properties for API_KEY.
   */
  public static ConnectorBuilder builder() {
    ConnectorBuilder builder = new ConnectorBuilder();
    RuntimeConfig.getString(ConnectorConfigKey.API_KEY).ifPresent(builder::withApiKey);
    // Default to long poll,
    String connectorMode = RuntimeConfig.getString(ConnectorConfigKey.CONNECTOR_MODE).orElse("LONG_POLL");
    switch (connectorMode) {
      case "TAG_ONLY":
        log.info("starting in connector in tag only mode");
        break;
      case "WEBHOOK":
        builder.useWebhook();
        break;
      case "LONG_POLL":
        builder.useFetchClient();
        break;
      default:
        // Checkstyle complained about fall-through...
        builder.useFetchClient();
        log.info("Unknown CONNECTOR_MODE option, starting with LONG_POLL");
    }
    RuntimeConfig.getString(ConnectorConfigKey.LONG_POLLING_LIMIT).map(Integer::parseInt)
        .ifPresent(builder::withFetchClientLimit);
    return builder;
  }
}
