/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import static io.wisetime.connector.controller.ConnectorControllerConfiguration.DEFAULT_ACTIVITY_TYPE_SYNC_INTERVAL_MINUTES;
import static io.wisetime.connector.controller.ConnectorControllerConfiguration.DEFAULT_ACTIVITY_TYPE_SYNC_SLOW_LOOP_INTERVAL_MINUTES;
import static io.wisetime.connector.controller.ConnectorControllerConfiguration.DEFAULT_TAG_SYNC_INTERVAL_MINUTES;
import static io.wisetime.connector.controller.ConnectorControllerConfiguration.DEFAULT_TAG_SYNC_SLOW_LOOP_INTERVAL_MINUTES;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
        IntervalConfig.builder().build()
    );
  }

  /**
   * @deprecated use IntervalConfig default or provide IntervalConfig object.
   */
  @Deprecated
  public ConnectorModule(ApiClient apiClient, ConnectorStore connectorStore, int tagSlowLoopIntervalMinutes) {
    this(apiClient,
        connectorStore,
        IntervalConfig.builder()
            .setTagSlowLoopIntervalMinutes(tagSlowLoopIntervalMinutes)
            .build()
    );
  }

  /**
   * Interval configuration for the connector. It is initially populated in the connector controller from the
   * {@link io.wisetime.connector.controller.ConnectorControllerConfiguration}.
   */
  @Builder(toBuilder = true, setterPrefix = "set")
  @Getter
  public static class IntervalConfig {

    @Builder.Default
    private int tagIntervalMinutes = DEFAULT_TAG_SYNC_INTERVAL_MINUTES;
    @Builder.Default
    private int activityTypeIntervalMinutes = DEFAULT_ACTIVITY_TYPE_SYNC_INTERVAL_MINUTES;

    @Builder.Default
    private int tagSlowLoopIntervalMinutes = DEFAULT_TAG_SYNC_SLOW_LOOP_INTERVAL_MINUTES;
    @Builder.Default
    private int activityTypeSlowLoopIntervalMinutes = DEFAULT_ACTIVITY_TYPE_SYNC_SLOW_LOOP_INTERVAL_MINUTES;
  }

  public int getTagIntervalMinutes() {
    return intervalConfig.getTagIntervalMinutes();
  }

  public int getTagSlowLoopIntervalMinutes() {
    return intervalConfig.getTagSlowLoopIntervalMinutes();
  }

  public int getActivityTypeIntervalMinutes() {
    return intervalConfig.getActivityTypeIntervalMinutes();
  }

  public int getActivityTypeSlowLoopIntervalMinutes() {
    return intervalConfig.getActivityTypeSlowLoopIntervalMinutes();
  }
}
