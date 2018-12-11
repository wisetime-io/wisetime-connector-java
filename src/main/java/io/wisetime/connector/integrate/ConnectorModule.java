/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.integrate;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;
import io.wisetime.connector.template.TemplateFormatter;

/**
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings("WeakerAccess")
public class ConnectorModule {

  private final ApiClient apiClient;
  private final TemplateFormatter publicTemplateFormatter;
  private final TemplateFormatter internalTemplateFormatter;
  private final ConnectorStore connectorStore;

  /**
   * Contains required dependencies needed for a WiseTime Connector to start.
   *
   * @param apiClient The client stub to call the WiseTime Connect APIs
   * @param publicTemplateFormatter The template formatter for general use. This formatter can be use to provide information
   *                                that do not have sensitive data.
   * @param internalTemplateFormatter The template formatter for providing more detailed information that might include
   *                                  sensitive data.
   * @param connectorStore Provides simple storage functionality.
   */
  public ConnectorModule(ApiClient apiClient,
                         TemplateFormatter publicTemplateFormatter,
                         TemplateFormatter internalTemplateFormatter,
                         ConnectorStore connectorStore) {
    this.apiClient = apiClient;
    this.publicTemplateFormatter = publicTemplateFormatter;
    this.internalTemplateFormatter = internalTemplateFormatter;
    this.connectorStore = connectorStore;
  }

  public ConnectorStore getConnectorStore() {
    return connectorStore;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public TemplateFormatter getPublicTemplateFormatter() {
    return publicTemplateFormatter;
  }

  public TemplateFormatter getInternalTemplateFormatter() {
    return internalTemplateFormatter;
  }
}
