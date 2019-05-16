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
import io.wisetime.connector.metric.ApiClientMetricWrapper;
import io.wisetime.connector.metric.WiseTimeConnectorMetricWrapper;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.health.WisetimeConnectorHealthIndicator;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
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
 * @author thomas.haines@practiceinsight.io, pascal.filippi@staff.wisetime.com
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
  private Runnable shutdownFunction;

  public ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    healthRunner = new HealthCheck();
    metricService = new MetricService();
    wiseTimeConnector = new WiseTimeConnectorMetricWrapper(configuration.getWiseTimeConnector(), metricService);
    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);

    final SQLiteHelper sqLiteHelper = new SQLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper));

    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper);

    tagRunner = new TagRunner(wiseTimeConnector);
    healthRunner.addHealthIndicator(tagRunner, timePoster,
        new WisetimeConnectorHealthIndicator(wiseTimeConnector));
  }

  private TimePoster createTimePoster(ConnectorControllerConfiguration configuration, ApiClient apiClient,
                                      SQLiteHelper sqLiteHelper) {
    ConnectorControllerBuilderImpl.LaunchMode launchMode = configuration.getLaunchMode();
    switch (launchMode) {
      case LONG_POLL:
        return new FetchClientTimePoster(wiseTimeConnector, apiClient, healthRunner,
            sqLiteHelper, configuration.getFetchClientLimit());
      case WEBHOOK:
        return new WebhookTimePoster(configuration.getWebhookPort(), wiseTimeConnector, metricService);
      case TAGS_ONLY:
      default:
        return new NoOpTimePoster();
    }
  }

  /**
   * Start the runner. This will run scheduled tasks for tag updates and health checks.
   * <p>
   * This method call is blocking, meaning that the current thread will wait until the application stops.
   */
  @Override
  public void start() throws Exception {
    initWiseTimeConnector();
    shutdownFunction = Thread.currentThread()::interrupt;
    healthRunner.setShutdownFunction(shutdownFunction);

    timePoster.start();

    Timer healthCheckTimer = new Timer("health-check-timer", true);
    healthCheckTimer.scheduleAtFixedRate(healthRunner, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(3));

    // start thread to monitor and upload new tags
    Timer tagTimer = new Timer("tag-check-timer", true);
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
    if (shutdownFunction == null) {
      throw new IllegalStateException("Connector hasn't been started");
    }
    shutdownFunction.run();
  }

  @Override
  public boolean isHealthy() {
    return healthRunner.checkConnectorHealth();
  }

  @Override
  public MetricInfo getMetrics() {
    return metricService.getMetrics();
  }

  //visible for testing
  void initWiseTimeConnector() {
    wiseTimeConnector.init(connectorModule);
  }
}
