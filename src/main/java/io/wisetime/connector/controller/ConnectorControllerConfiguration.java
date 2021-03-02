/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;

/**
 * Configuration class for {@link ConnectorControllerImpl}.
 *
 * @author vadym
 */
public interface ConnectorControllerConfiguration {

  WiseTimeConnector getWiseTimeConnector();

  ApiClient getApiClient();

  boolean isForcePersistentStorage();

  ConnectorControllerBuilderImpl.PostedTimeLoadMode getPostedTimeLoadMode();

  ConnectorControllerBuilderImpl.TagScanMode getTagScanMode();

  ConnectorControllerBuilderImpl.ActivityTypeScanMode getActivityTypeScanMode();

  int getWebhookPort();

  int getFetchClientLimit();
}
