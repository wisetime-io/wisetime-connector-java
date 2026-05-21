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

  int DEFAULT_TAG_SYNC_INTERVAL_MINUTES = 1;

  int DEFAULT_TAG_SYNC_SLOW_LOOP_INTERVAL_MINUTES = 5;

  int DEFAULT_ACTIVITY_TYPE_SYNC_INTERVAL_MINUTES = 5;

  int DEFAULT_ACTIVITY_TYPE_SYNC_SLOW_LOOP_INTERVAL_MINUTES = 15;

  WiseTimeConnector getWiseTimeConnector();

  ApiClient getApiClient();

  boolean isForcePersistentStorage();

  ConnectorControllerBuilderImpl.PostedTimeLoadMode getPostedTimeLoadMode();

  ConnectorControllerBuilderImpl.TagScanMode getTagScanMode();

  ConnectorControllerBuilderImpl.ActivityTypeScanMode getActivityTypeScanMode();

  int getFetchClientLimit();

  int getTagSyncIntervalMinutes();

  int getTagSyncSlowLoopIntervalMinutes();

  int getActivityTypeSyncIntervalMinutes();

  int getActivityTypeSyncSlowLoopIntervalMinutes();
}
