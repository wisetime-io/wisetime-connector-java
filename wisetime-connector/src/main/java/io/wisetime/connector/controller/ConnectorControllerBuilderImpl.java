/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import com.google.common.base.Preconditions;
import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.ConnectorController.Builder;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.DefaultApiClient;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.RuntimeConfigKey;
import java.util.Optional;
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
  @Getter
  private WiseTimeConnector wiseTimeConnector;
  @Getter
  private ApiClient apiClient;
  private String apiKey;
  private TagScanMode tagScanMode = TagScanMode.ENABLED;
  private ActivityTypeScanMode activityTypeScanMode = ActivityTypeScanMode.ENABLED;
  private PostedTimeLoadMode postedTimeLoadMode = PostedTimeLoadMode.LONG_POLL;

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
    postedTimeLoadMode = PostedTimeLoadMode.LONG_POLL;
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
  public Builder disablePostedTimeFetching() {
    postedTimeLoadMode = PostedTimeLoadMode.DISABLED;
    return this;
  }

  @Override
  public ConnectorController.Builder useTagsOnly() {
    this.postedTimeLoadMode = PostedTimeLoadMode.DISABLED;
    this.activityTypeScanMode = ActivityTypeScanMode.DISABLED;
    this.tagScanMode = TagScanMode.ENABLED;
    return this;
  }

  @Override
  public Builder disableTagScan() {
    this.tagScanMode = TagScanMode.DISABLED;
    return this;
  }

  @Override
  public Builder disableActivityTypesScan() {
    this.activityTypeScanMode = ActivityTypeScanMode.DISABLED;
    return this;
  }

  @Override
  public ConnectorController build() {
    Preconditions.checkNotNull(wiseTimeConnector,
        "an implementation of '%s' interface must be supplied",
        WiseTimeConnector.class.getSimpleName());

    checkDeprecatedKey(() -> "AWS_ACCESS_KEY_ID");
    checkDeprecatedKey(() -> "AWS_SECRET_ACCESS_KEY");
    checkDeprecatedKey(() -> "AWS_REGION");

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
  public PostedTimeLoadMode getPostedTimeLoadMode() {
    final Optional<PostedTimeLoadMode> postedTimeMode = RuntimeConfig.getString(ConnectorConfigKey.RECEIVE_POSTED_TIME)
        .map(PostedTimeLoadMode::valueOf);
    return postedTimeMode.orElseGet(() -> postedTimeLoadMode);
  }

  @Override
  public TagScanMode getTagScanMode() {
    return RuntimeConfig.getString(ConnectorConfigKey.TAG_SCAN)
        .map(TagScanMode::valueOf)
        .orElse(tagScanMode);
  }

  @Override
  public ActivityTypeScanMode getActivityTypeScanMode() {
    return RuntimeConfig.getString(ConnectorConfigKey.ACTIVITY_TYPE_SCAN)
        .map(ActivityTypeScanMode::valueOf)
        .orElse(activityTypeScanMode);
  }

  @Override
  public int getFetchClientLimit() {
    return RuntimeConfig.getInt(ConnectorConfigKey.LONG_POLL_BATCH_SIZE).orElse(fetchClientFetchLimit);
  }

  private void checkDeprecatedKey(RuntimeConfigKey configKey) {
    if (RuntimeConfig.getString(configKey).isPresent()) {
      log.warn("{} configuration setting has been deprecated and no longer have any effect. "
          + "Please remove these from the connector configuration. "
          + "Connector logging is now automatically set up via Connect API.", configKey);
    }
  }

  public enum PostedTimeLoadMode {
    LONG_POLL, DISABLED
  }

  public enum TagScanMode {
    ENABLED, DISABLED
  }

  public enum ActivityTypeScanMode {
    ENABLED, DISABLED
  }
}
