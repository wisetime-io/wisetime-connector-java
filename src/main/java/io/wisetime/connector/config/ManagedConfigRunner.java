/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import com.google.common.annotations.VisibleForTesting;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.log.LogbackConfigurator;
import io.wisetime.connector.utils.RuntimeEnvironmentUtil;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.LoggerFactory;

/**
 * Retrieve the configuration particulars to support the managed connector service.
 * <p>
 * Enforces a singleton runner pattern in the event that the previous managed config request has not completed prior to
 * the next scheduled check.
 *
 * @author dchandler
 */
@Slf4j
public class ManagedConfigRunner extends TimerTask implements HealthIndicator {

  private static final int MAX_MINS_SINCE_SUCCESS_DEFAULT = 60;

  private static final int MANAGED_SERVICE_RENEWAL_THRESHOLD_MINS = 20;

  private final AtomicBoolean runLock = new AtomicBoolean(false);

  private final int maxMinsSinceSuccess;

  private final WiseTimeConnector wiseTimeConnector;

  private final ApiClient apiClient;

  private final ConnectorInfoProvider connectorInfoProvider;

  DateTime lastSuccessfulRun;

  ZonedDateTime cachedServiceExpiryDate;

  public ManagedConfigRunner(WiseTimeConnector wiseTimeConnector,
      ApiClient apiClient,
      ConnectorInfoProvider connectorInfoProvider) {

    this.wiseTimeConnector = wiseTimeConnector;
    this.apiClient = apiClient;
    this.connectorInfoProvider = connectorInfoProvider;

    this.lastSuccessfulRun = DateTime.now();
    maxMinsSinceSuccess = RuntimeConfig
        .getInt(ConnectorConfigKey.HEALTH_MAX_MINS_SINCE_SUCCESS)
        .orElse(MAX_MINS_SINCE_SUCCESS_DEFAULT);
  }

  @Override
  public void run() {
    if (!runLock.compareAndSet(false, true)) {
      log.info("Skip manage config timer instantiation, previous manage config process is yet to complete");
      return;
    }

    final ZoneId zoneId = ZoneId.of(connectorInfoProvider.get().getClientTimeZoneOffset());

    try {
      // Request a new managed config when the connector service has expired
      if (cachedServiceExpiryDate != null) {
        final ZonedDateTime managedConfigGetExpiryThreshold =
            ZonedDateTime.now(zoneId).plusMinutes(MANAGED_SERVICE_RENEWAL_THRESHOLD_MINS);

        if (managedConfigGetExpiryThreshold.isAfter(cachedServiceExpiryDate)) {
          cachedServiceExpiryDate = onManagedConfigResponse(
              apiClient.getTeamManagedConfig(createManageConfigRequest()), zoneId);
        }

      } else {
        cachedServiceExpiryDate = onManagedConfigResponse(
            apiClient.getTeamManagedConfig(createManageConfigRequest()), zoneId);
      }
      onSuccessfulManageConfigResponse();

    } catch (Exception e) {
      LoggerFactory.getLogger(LogbackConfigurator.class).error(e.getMessage(), e);

    } finally {
      // ensure lock is released
      runLock.set(false);
    }
  }

  @VisibleForTesting
  ZonedDateTime onManagedConfigResponse(ManagedConfigResponse configResponse, ZoneId zoneId) {
    LogbackConfigurator.configureBaseLogging(configResponse);
    return toExpiryZoneDateTime(configResponse.getServiceIdExpiry(), zoneId);
  }

  @VisibleForTesting
  ZonedDateTime toExpiryZoneDateTime(Date serviceExpiry, ZoneId zoneId) {
    return serviceExpiry.toInstant().atZone(zoneId);
  }

  private void onSuccessfulManageConfigResponse() {
    lastSuccessfulRun = DateTime.now();
  }

  @Override
  public boolean isHealthy() {
    return Minutes.minutesBetween(lastSuccessfulRun, DateTime.now()).getMinutes() < maxMinsSinceSuccess;
  }

  @VisibleForTesting
  ManagedConfigRequest createManageConfigRequest() {
    return new ManagedConfigRequest()
        .connectorType(wiseTimeConnector.getConnectorType())
        .environment(RuntimeEnvironmentUtil.getEnvProperties())
        .connectorLibraryVersion(RuntimeEnvironmentUtil.getLibraryImplVersion())
        .clientTimeZoneOffset(connectorInfoProvider.get().getClientTimeZoneOffset())
        .clientTimestamp((double) Instant.now().getEpochSecond());
  }
}
