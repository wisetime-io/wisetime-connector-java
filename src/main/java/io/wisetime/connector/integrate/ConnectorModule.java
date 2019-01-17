/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.integrate;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;

/**
 * Module with main WiseTime connector dependencies.
 *
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings("WeakerAccess")
public class ConnectorModule {

  private final ApiClient apiClient;
  private final ConnectorStore connectorStore;

  public ConnectorModule(ApiClient apiClient,
                         ConnectorStore connectorStore) {
    this.apiClient = apiClient;
    this.connectorStore = connectorStore;
  }

  public ConnectorStore getConnectorStore() {
    return connectorStore;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

}
