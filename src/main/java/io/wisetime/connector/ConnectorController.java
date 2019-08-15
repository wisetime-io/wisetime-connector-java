/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.controller.ConnectorControllerBuilderImpl;
import io.wisetime.connector.metric.MetricInfo;

/**
 * Main entry point of WiseTime connector. Sample usage:
 * <pre>
 *      ConnectorController controller = ConnectorController.newBuilder()
 *          .useWebhook()
 *          .withWiseTimeConnector(myConnector)
 *          .withApiKey(apiKey)
 *          .build();
 *  </pre>
 *
 * Provides the ability to manage connector (e.g. start/stop) and check its work (e.g. health and metrics)
 *
 * You have to provide implementation of {@link WiseTimeConnector} when creating ConnectorController instance.
 *
 * More information regarding the API key can be found here:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 *
 * @author yehor.lashkul
 */
public interface ConnectorController {

  /**
   * Create instance of connector {@link ConnectorController}.
   */
  static Builder newBuilder() {
    return new ConnectorControllerBuilderImpl();
  }

  /**
   * Starts the connector.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   *
   * @throws Exception if any error occurred during start up
   */
  void start() throws Exception;

  /**
   * Stops previously run connector.
   */
  void stop();

  /**
   * Checks if connector is healthy.
   *
   * @return whether the connector is healthy
   */
  boolean isHealthy();

  /**
   * Returns metrics collected from the start of the application.
   *
   * @return {@link MetricInfo} object as a representation of the collected metrics
   */
  MetricInfo getMetrics();

  /**
   * Builder for {@link ConnectorController}. You have to provide an API key or custom {@link ApiClient} that will handle
   * authentication. For more information on how to obtain an API key, refer to:
   * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
   * <p>
   * You have to set a connector by calling {@link #withWiseTimeConnector(WiseTimeConnector)}.
   * You can override configured values at runtime by setup system properties or environment variable. You can find list
   * of allowed values here: {@link io.wisetime.connector.config.ConnectorConfigKey}.
   */
  interface Builder {

    /**
     * Implementation of {@link WiseTimeConnector} is required to start runner.
     *
     * @param wiseTimeConnector see {@link WiseTimeConnector}
     */
    Builder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector);

    /**
     * Custom implementation of {@link ApiClient}. You have to provide api key or {@link ApiClient}
     * for successful authorization. If both values are set ApiClient takes precedence.
     *
     * @see #withApiKey(String)
     */
    Builder withApiClient(ApiClient apiClient);

    /**
     * More information about WiseTime API key: <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
     * <p>
     * You have to provide api key or custom implementation of {@link ApiClient} for successful authorization.
     *
     * @see #withApiClient(ApiClient)
     */
    Builder withApiKey(String apiKey);

    /**
     * If persistent storage is required, this property forces the operator to set the DATA_DIR configuration parameter (via
     * system property or environment variable). The DATA_DIR directory will be used to persist data across connector
     * restarts.
     * <p>
     * Otherwise the connector will not force the operator to specify a DATA_DIR. It will still use the DATA_DIR path if
     * one is set. If none is provided, a subdirectory in the /tmp/ path will be used.
     * <p>
     * Default is false.
     *
     * @param forcePersistentStorage whether persistent storage is required for this connector or not.
     */
    Builder requirePersistentStorage(boolean forcePersistentStorage);

    /**
     * Instructs ConnectorController do not start long polling connection to WiseTime server nor web server for webhook.
     * Posted time will be ignored if connector is running in this mode.
     *
     * @see #useWebhook()
     * @see #useFetchClient()
     */
    Builder disablePostedTimeFetching();

    /**
     * Instructs ConnectorController to start in long polling mode (this is default value).
     * Other options are: webhook or tag only mode when posted time groups are not collected by connector.
     *
     * @see #useWebhook()
     * @see #useTagsOnly()
     */
    Builder useFetchClient();

    /**
     * Set connector to launch in long polling mode mode. Max number of time groups to fetch per request will be
     * configured according to provided limit. Should be in range from 1 to 25.
     *
     * @see #useFetchClient()
     */
    Builder withFetchClientLimit(int limit);

    /**
     * Instructs ConnectorController to start a webserver to accept posted time groups. By default server is started
     * on port 8080. You can set custom port by {@link #withWebhookPort(int)}.
     *
     * @see #useFetchClient()
     * @see #useTagsOnly()
     */
    Builder useWebhook();

    /**
     * Set connector to launch in webhook mode. Server will be launch on the provided port.
     *
     * @see #useWebhook()
     */
    Builder withWebhookPort(int port);

    /**
     * Instructs ConnectorController not to processed time groups (neither by long polling mechanism nor webhook).
     * Connector will create tags in Wisetime only.
     *
     * @see #useFetchClient()
     * @see #useWebhook() ()
     */
    Builder useTagsOnly();

    /**
     * Disable tag scan in external system and uploading them to WiseTime. Enabled by default.
     */
    Builder disableTagScan();

    /**
     * Build {@link ConnectorController}. Make sure to set {@link WiseTimeConnector} and apiKey or apiClient before calling
     * this method.
     */
    ConnectorController build();
  }
}
