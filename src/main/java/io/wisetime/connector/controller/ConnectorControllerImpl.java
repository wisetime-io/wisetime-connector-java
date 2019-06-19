/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import io.wisetime.connector.tag.ApiClientTagWrapper;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.ConnectorModule;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.JsonPayloadService;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.connector.config.info.ConstantConnectorInfoProvider;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.health.WiseTimeConnectorHealthIndicator;
import io.wisetime.connector.log.LogbackConfigurator;
import io.wisetime.connector.metric.ApiClientMetricWrapper;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.metric.WiseTimeConnectorMetricWrapper;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.time_poster.NoOpTimePoster;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.long_polling.FetchClientTimePoster;
import io.wisetime.connector.time_poster.webhook.WebhookTimePoster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ConnectorController}
 *
 * @author thomas.haines
 * @author pascal.filippi
 */
@Slf4j
@RequiredArgsConstructor
public class ConnectorControllerImpl implements ConnectorController, HealthIndicator {

  private final TimePoster timePoster;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;
  private final TagRunner tagRunner;
  private final HealthCheck healthRunner;
  private final MetricService metricService;
  private final Timer healthCheckTimer;
  private final Timer tagTimer;
  private Status status = Status.STOPPED;

  public ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    healthRunner = new HealthCheck();
    metricService = new MetricService();
    wiseTimeConnector = new WiseTimeConnectorMetricWrapper(configuration.getWiseTimeConnector(), metricService);
    tagRunner = new TagRunner(wiseTimeConnector);
    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);
    apiClient = new ApiClientTagWrapper(apiClient, tagRunner);

    // add base runtime logging to standard logging output
    LogbackConfigurator.configureBaseLogging(apiClient);

    final SQLiteHelper sqLiteHelper = new SQLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper));

    final ConnectorInfoProvider connectorInfoProvider = new ConstantConnectorInfoProvider();
    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper, connectorInfoProvider);

    healthRunner.addHealthIndicator(tagRunner, timePoster, new WiseTimeConnectorHealthIndicator(wiseTimeConnector));
    healthCheckTimer = new Timer("health-check-timer", false);
    tagTimer = new Timer("tag-check-timer", true);
  }

  /**
   * <pre>
   *  Start the runner.
   *  This will run tasks connected to tag updates, posted time and health checks.
   *  This method call is blocking, meaning that the current thread will wait until the application stops.
   * </pre>
   */
  @Override
  public void start() throws Exception {
    if (status != Status.STOPPED) {
      log.warn("Cannot start the connector while it is still running. Start attempt ignored.");
      return;
    }
    status = Status.STARTING;

    if (initWiseTimeConnector()) {
      timePoster.start();
      healthRunner.setShutdownFunction(Thread.currentThread()::interrupt);
      healthCheckTimer
          .scheduleAtFixedRate(healthRunner, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(3));
      tagTimer
          .scheduleAtFixedRate(tagRunner, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));
      status = Status.STARTED;

      while (timePoster.isRunning()) {
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          break;
        }
      }
      stop();
    }
  }

  @Override
  public void stop() {
    status = Status.STOPPING;
    try {
      healthRunner.cancel();
      healthCheckTimer.cancel();
      healthCheckTimer.purge();
      tagTimer.cancel();
      tagTimer.purge();
      timePoster.stop();
    } catch (Exception e) {
      log.warn("There was an error while stopping the connector", e);
    } finally {
      wiseTimeConnector.shutdown();
      status = Status.STOPPED;
    }
  }

  @Override
  public boolean isHealthy() {
    return healthRunner.checkConnectorHealth();
  }

  @Override
  public MetricInfo getMetrics() {
    return metricService.getMetrics();
  }

  private boolean initWiseTimeConnector() {
    try {
      wiseTimeConnector.init(connectorModule);
      return true;
    } catch (Exception e) {
      if (status == Status.STOPPING) {
        log.debug("Connector initialisation aborted. Connector is stopping.", e);
      } else {
        log.error("Failed to initialise the connector", e);
      }
      return false;
    }
  }

  private TimePoster createTimePoster(ConnectorControllerConfiguration configuration,
                                      ApiClient apiClient,
                                      SQLiteHelper sqLiteHelper,
                                      ConnectorInfoProvider connectorInfoProvider) {
    final ConnectorControllerBuilderImpl.LaunchMode launchMode = configuration.getLaunchMode();
    switch (launchMode) {
      case LONG_POLL:
        return new FetchClientTimePoster(
            wiseTimeConnector,
            apiClient,
            healthRunner,
            sqLiteHelper,
            configuration.getFetchClientLimit());
      case WEBHOOK:
        return new WebhookTimePoster(
            configuration.getWebhookPort(),
            new JsonPayloadService(connectorInfoProvider, TolerantObjectMapper.create()),
            wiseTimeConnector,
            metricService);
      case TAGS_ONLY:
        return new NoOpTimePoster();
      default:
        log.error("unknown launchMode={}, defaulting to {}", launchMode, NoOpTimePoster.class.getName());
        return new NoOpTimePoster();
    }
  }

  private enum Status {
    STARTING,
    STARTED,
    STOPPING,
    STOPPED
  }
}
