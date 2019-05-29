/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import com.google.common.base.Preconditions;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author vadym
 */
@Slf4j
public class ConnectorControllerBuilderImpl implements ConnectorController.Builder,
    ConnectorControllerConfiguration {

  @Getter
  private boolean forcePersistentStorage = false;
  private int fetchClientFetchLimit = 25;
  private int longPollingThreads = 2;
  @Getter
  private WiseTimeConnector wiseTimeConnector;
  @Getter
  private ApiClient apiClient;
  private String apiKey;
  private int webhookPort = 8080;
  private LaunchMode launchMode = LaunchMode.LONG_POLL;

  @Override
  public ConnectorController.Builder withWiseTimeConnector(WiseTimeConnector wiseTimeConnector) {
    this.wiseTimeConnector = wiseTimeConnector;
    return this;
  }

  @Override
  public ConnectorController.Builder withApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
    return this;
  }

  @Override
  public ConnectorController.Builder withApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  @Override
  public ConnectorController.Builder requirePersistentStorage(boolean persistentStorageOnly) {
    this.forcePersistentStorage = persistentStorageOnly;
    return this;
  }

  @Override
  public ConnectorController.Builder useFetchClient() {
    launchMode = LaunchMode.LONG_POLL;
    return this;
  }

  @Override
  public ConnectorController.Builder withFetchClientLimit(int limit) {
    useFetchClient();
    if (limit < 1) {
      log.warn("Invalid fetch client limit. It has to be in range from 1 to 25. Provided value: {}. It will be reset to 1",
          limit);
      fetchClientFetchLimit = 1;
    } else if (limit > 25) {
      log.warn("Invalid fetch client limit. It has to be in range from 1 to 25. Provided value: {}. It will be reset to 25",
          limit);
      fetchClientFetchLimit = 25;
    }
    this.fetchClientFetchLimit = limit;
    return this;
  }

  @Override
  public ConnectorController.Builder useWebhook() {
    launchMode = LaunchMode.WEBHOOK;
    return this;
  }

  @Override
  public ConnectorController.Builder withWebhookPort(int port) {
    useWebhook();
    this.webhookPort = port;
    return this;
  }

  @Override
  public ConnectorController.Builder useTagsOnly() {
    launchMode = LaunchMode.TAGS_ONLY;
    return this;
  }

  @Override
  public ConnectorController build() {
    Preconditions.checkNotNull(wiseTimeConnector,
        "an implementation of '%s' interface must be supplied",
        WiseTimeConnector.class.getSimpleName());

    if (apiClient == null) {
      String apiKey = RuntimeConfig.getString(ConnectorConfigKey.API_KEY)
          .orElse(this.apiKey);

      Preconditions.checkNotNull(apiKey,
          "an apiKey must be supplied via constructor or environment parameter to use with the default apiClient");

      apiClient = new DefaultApiClient(apiKey);
    }
    return new ConnectorControllerImpl(this);
  }

  @Override
  public LaunchMode getLaunchMode() {
    return RuntimeConfig.getString(ConnectorConfigKey.CONNECTOR_MODE)
        .map(ConnectorControllerBuilderImpl.LaunchMode::valueOf)
        .orElse(launchMode);
  }

  @Override
  public int getWebhookPort() {
    return RuntimeConfig.getInt(ConnectorConfigKey.WEBHOOK_PORT).orElse(webhookPort);
  }

  @Override
  public int getFetchClientLimit() {
    return RuntimeConfig.getInt(ConnectorConfigKey.LONG_POLL_BATCH_SIZE).orElse(fetchClientFetchLimit);
  }

  public enum LaunchMode {
    LONG_POLL, WEBHOOK, TAGS_ONLY
  }
}
