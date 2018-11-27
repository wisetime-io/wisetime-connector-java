/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.integrate;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.StorageManager;
import io.wisetime.connector.template.TemplateFormatter;

/**
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings("WeakerAccess")
public class ConnectorModule {

  private final ApiClient apiClient;
  private final TemplateFormatter templateFormatter;
  private final StorageManager storageManager;

  public ConnectorModule(ApiClient apiClient,
                         TemplateFormatter templateFormatter,
                         StorageManager storageManager) {
    this.apiClient = apiClient;
    this.templateFormatter = templateFormatter;
    this.storageManager = storageManager;
  }

  public StorageManager getStorageManager() {
    return storageManager;
  }

  public ApiClient getApiClient() {
    return apiClient;
  }

  public TemplateFormatter getTemplateFormatter() {
    return templateFormatter;
  }
}
