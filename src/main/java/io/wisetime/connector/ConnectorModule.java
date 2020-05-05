/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Module with main WiseTime connector dependencies.
 *
 * @author thomas.haines
 */
@RequiredArgsConstructor
@Getter
public class ConnectorModule {

  private final ApiClient apiClient;
  private final ConnectorStore connectorStore;
  private final int tagSlowLoopIntervalMinutes;

  public int getTagSlowLoopIntervalMinutes() {
    return tagSlowLoopIntervalMinutes;
  }
}
