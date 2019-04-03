/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import org.apache.commons.lang3.StringUtils;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.postedtime.fetchclient.FetchClientManager;
import io.wisetime.connector.postedtime.webhook.WebhookServer;

/**
 * Main entry point of the WiseTime Connector.
 * <p>
 * Example connector that only uploads tags to WiseTime and does not receive posted time:
 * <pre>
 * ConnectorRunner runner = ConnectorRunner.Builder()
 *     .withWiseTimeConnector(myConnector)
 *     .withApiKey(myApiKey)
 *     .build();
 * </pre>
 *
 * Example connector that provides a webhook to receive posted time:
 * <pre>
 * ConnectorRunner runner = ConnectorRunner.Builder()
 *     .withWiseTimeConnector(myConnector)
 *     .withApiKey(myApiKey)
 *     .withWebhook(8080)
 *     .build();
 * </pre>
 *
 * Example connector that uses a fetch client that automatically polls for posted time:
 * <pre>
 * ConnectorRunner runner = ConnectorRunner.Builder()
 *     .withWiseTimeConnector(myConnector)
 *     .withApiKey(myApiKey)
 *     .withFetchClient("myFetchClientId")
 *     .build();
 * </pre>
 *
 * You can then run the connector by calling {@link #start()}. This is a blocking call, meaning that the current thread
 * will wait until the application dies.
 * <p>
 * Main extension point is {@link WiseTimeConnector}.
 * <p>
 * More information regarding the API key can be found here:
 * <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
 *
 * @author shane.xie@practiceinsight.io
 */
public class ConnectorRunner {

  private WiseTimeConnector wiseTimeConnector;
  private ConnectorModule connectorModule;
  private WebhookServer webhookServer;
  private FetchClientManager fetchClientManager;

  public void start() {
    wiseTimeConnector.init(connectorModule);

    if (hasWebhookServer()) {
      // Start webhook server
    }
    if (hasFetchClientManager()) {
      // Start fetch client manager
    }
  }

  private ConnectorRunner(final WiseTimeConnector wiseTimeConnector,
                          final ConnectorModule connectorModule) {
    // TODO(SX)
  }

  private ConnectorRunner(final WebhookServer webhookServer,
                          final WiseTimeConnector wiseTimeConnector,
                          final ConnectorModule connectorModule) {
    // TODO(SX)
  }

  private ConnectorRunner(final FetchClientManager fetchClientManager,
                          final WiseTimeConnector wiseTimeConnector,
                          final ConnectorModule connectorModule) {
    // TODO(SX)
  }

  private boolean hasWebhookServer() {
    return webhookServer != null;
  }

  private boolean hasFetchClientManager() {
    return hasFetchClientManager();
  }

  public static class Builder {

    private WiseTimeConnector wiseTimeConnector;
    private String apiKey;
    private ApiClient apiClient;
    private String fetchClientId;
    private Integer webhookPort;
    private boolean onlyUseSlf4J = false;

    /**
     * Build {@link ConnectorRunner}. Make sure to set {@link WiseTimeConnector} and apiKey before calling this method.
     */
    public ConnectorRunner build() {

      if (!hasApiClient()) {
        if (!hasApiKey()) {
          throw new IllegalArgumentException(
              "An API key must be supplied via ConnectorRunner.Builder or environment variable to use the default " +
                  "ApiClient");
        }
        RestRequestExecutor requestExecutor = new RestRequestExecutor(apiKey);
        apiClient = new DefaultApiClient(requestExecutor);
      }

     if (hasFetchClient() && hasWebhook()) {
        throw new IllegalStateException(
            "You can configure the connector with wither a fetch client or webhook but not both");
      }

      if (!hasWiseTimeConnector()) {
        throw new IllegalArgumentException(String.format("An implementation of the '%s' interface must be provided",
            WiseTimeConnector.class.getSimpleName()));
      }

      if (hasWebhook()) {
        return new ConnectorRunner(webhookServer, wiseTimeConnector, connectorModule);
      } else if (hasFetchClient()) {
        return new ConnectorRunner(fetchClientManager, wiseTimeConnector, connectorModule);
      }

      // No configured posted time handler.
      return new ConnectorRunner(wiseTimeConnector, connectorModule);
    }

    /**
     * Implementation of {@link WiseTimeConnector} is required to start server.
     *
     * @param wiseTimeConnector see {@link WiseTimeConnector}
     */
    public Builder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector) {
      this.wiseTimeConnector = wiseTimeConnector;
      return this;
    }

    /**
     * More information about WiseTime API key: <a href="https://wisetime.io/docs/connect/api/">WiseTime Connect API</a>.
     * <p>
     * You have to provide api key or custom implementation of {@link ApiClient} for successful authorization.
     *
     * @param apiKey see {@link #withApiClient(ApiClient)}
     */
    public Builder withApiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Configure a fetch client to poll the WiseTime Connect API for posted time.
     * <p>
     * You can configure the connector with either a fetch client or a webhook but not both.
     *
     * @param fetchClientId
     */
    public Builder withFetchClient(String fetchClientId) {
      this.fetchClientId = fetchClientId;
      return this;
    }

    /**
     * Configure a webhook HTTP server to receive posted time at the webhookPort.
     * <p>
     * You can configure the connector with either a fetch client or a webhook but not both.
     *
     * @param webhookPort
     */
    public Builder withWebhook(int webhookPort) {
      this.webhookPort = webhookPort;
      return this;
    }

    /**
     * Use a custom implementation of {@link ApiClient}. If set the API key property is ignored.
     *
     * @param apiClient see {@link #withApiKey(String)}
     */
    Builder withApiClient(ApiClient apiClient) {
      this.apiClient = apiClient;
      return this;
    }

    Builder onlyUseSlf4J(boolean onlyUseSlf4J) {
      this.onlyUseSlf4J = onlyUseSlf4J;
      return this;
    }

    private boolean hasWiseTimeConnector() {
      return wiseTimeConnector != null;
    }

    private boolean hasApiKey() {
      return StringUtils.isEmpty(apiKey);
    }

    private boolean hasApiClient() {
      return apiClient != null;
    }

    private boolean hasWebhook() {
      return webhookPort != null;
    }

    private boolean hasFetchClient() {
      return fetchClientId != null;
    }
  }
}
