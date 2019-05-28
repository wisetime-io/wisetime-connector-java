/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.ConnectorModule;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.health.WisetimeConnectorHealthIndicator;
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

  public ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    healthRunner = new HealthCheck();
    metricService = new MetricService();
    wiseTimeConnector = new WiseTimeConnectorMetricWrapper(configuration.getWiseTimeConnector(), metricService);
    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);

    // add base runtime logging to standard logging output
    LogbackConfigurator.configureBaseLogging(apiClient);

    final SQLiteHelper sqLiteHelper = new SQLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper));

    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper);

    tagRunner = new TagRunner(wiseTimeConnector);
    healthRunner.addHealthIndicator(tagRunner, timePoster,
        new WisetimeConnectorHealthIndicator(wiseTimeConnector));
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
    healthRunner.setShutdownFunction(Thread.currentThread()::interrupt);
    Timer healthCheckTimer = new Timer("health-check-timer", true);
    Timer tagTimer = new Timer("tag-check-timer", true);

    initWiseTimeConnector();
    timePoster.start();

    // this is a user thread (non-daemon), as health check should prolong VM shutdown
    Timer healthCheckTimer = new Timer("health-check-timer", false);
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer", true);
    // TODO(Dev) this should run in executor to prevent thread exhaustion
    tagTimer.scheduleAtFixedRate(tagRunner, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5));

    while (timePoster.isRunning()) {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        break;
      }
    }
    healthCheckTimer.cancel();
    healthCheckTimer.purge();
    tagTimer.cancel();
    tagTimer.purge();
    try {
      timePoster.stop();
    } catch (Exception e) {
      log.warn("Failed to stop time poster runner", e);
    }
    wiseTimeConnector.shutdown();
  }

  @Override
  public void stop() {
    try {
      healthRunner.cancel();
      timePoster.stop();
      wiseTimeConnector.shutdown();
    } catch (Exception e) {
      log.error("Exception while stopping the connector", e);
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

  // Visible for testing
  void initWiseTimeConnector() {
    wiseTimeConnector.init(connectorModule);
  }


  private TimePoster createTimePoster(ConnectorControllerConfiguration configuration,
                                      ApiClient apiClient,
                                      SQLiteHelper sqLiteHelper) {
    final ConnectorControllerBuilderImpl.LaunchMode launchMode = configuration.getLaunchMode();
    switch (launchMode) {
      case LONG_POLL:
        return new FetchClientTimePoster(
            wiseTimeConnector,
            apiClient,
            healthRunner,
            sqLiteHelper,
            configuration.getFetchClientLimit(),
            configuration.getLongPollingThreads());
      case WEBHOOK:
        return new WebhookTimePoster(
            configuration.getWebhookPort(),
            wiseTimeConnector,
            metricService);
      case TAGS_ONLY:
        return new NoOpTimePoster();
      default:
        log.error("unknown launchMode={}, defaulting to {}", launchMode, NoOpTimePoster.class.getName());
        return new NoOpTimePoster();
    }
  }

}
