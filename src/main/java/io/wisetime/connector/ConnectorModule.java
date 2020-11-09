/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Module with main WiseTime connector dependencies.
 *
 * @author thomas.haines, shane.xie, vadym.sokulen, yehor.lashkul
 */
@RequiredArgsConstructor
@Getter
public class ConnectorModule {

  private final ApiClient apiClient;
  private final ConnectorStore connectorStore;
  private final IntervalConfig intervalConfig;

  /**
   * Preferred constructor using default IntervalConfig values.
   */
  public ConnectorModule(ApiClient apiClient, ConnectorStore connectorStore) {
    this(apiClient,
        connectorStore,
        // use defaults
        new IntervalConfig()
    );
  }

  /**
   * @deprecated use IntervalConfig default or provide IntervalConfig object.
   */
  @Deprecated
  public ConnectorModule(ApiClient apiClient, ConnectorStore connectorStore, int tagSlowLoopIntervalMinutes) {
    this(apiClient,
        connectorStore,
        new IntervalConfig().setTagSlowLoopIntervalMinutes(tagSlowLoopIntervalMinutes)
    );
  }

  @Data
  @Accessors(chain = true)
  public static class IntervalConfig {

    int tagSlowLoopIntervalMinutes = 5;
    int activityTypeSlowLoopIntervalMinutes = 5;
  }

}
