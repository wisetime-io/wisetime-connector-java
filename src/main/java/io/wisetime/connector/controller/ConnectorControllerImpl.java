/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.ConnectorModule;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.JsonPayloadService;
import io.wisetime.connector.config.ManagedConfigRunner;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.connector.config.info.ConstantConnectorInfoProvider;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.health.WiseTimeConnectorHealthIndicator;
import io.wisetime.connector.metric.ApiClientMetricWrapper;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.metric.WiseTimeConnectorMetricWrapper;
import io.wisetime.connector.tag.ApiClientTagWrapper;
import io.wisetime.connector.tag.NoOpTagRunner;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.time_poster.NoOpTimePoster;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.long_polling.FetchClientTimePoster;
import io.wisetime.connector.time_poster.webhook.WebhookTimePoster;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ConnectorController}
 *
 * @author thomas.haines
 * @author pascal.filippi
 * @author shane.xie
 */
@Slf4j
@RequiredArgsConstructor
public class ConnectorControllerImpl implements ConnectorController, HealthIndicator {

  private final TimePoster timePoster;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;

  private final TagRunner tagRunner;
  private final HealthCheck healthRunner;
  private final ManagedConfigRunner managedConfigRunner;

  private final MetricService metricService;

  private final Timer healthCheckTimer;
  private final Timer tagTimer;
  private final Timer managedConfigTimer;

  // Awaitable, cancellable connector run
  private CompletableFuture<Void> connectorRun;

  ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    healthRunner = new HealthCheck();
    metricService = new MetricService();
    wiseTimeConnector = new WiseTimeConnectorMetricWrapper(configuration.getWiseTimeConnector(), metricService);

    tagRunner = createTagRunner(configuration, wiseTimeConnector);

    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);
    apiClient = new ApiClientTagWrapper(apiClient, tagRunner);

    final SQLiteHelper sqLiteHelper = new SQLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper));

    final ConnectorInfoProvider connectorInfoProvider = new ConstantConnectorInfoProvider();
    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper, connectorInfoProvider);

    managedConfigRunner = new ManagedConfigRunner(wiseTimeConnector, apiClient, connectorInfoProvider);

    healthRunner.addHealthIndicator(tagRunner,
        timePoster,
        managedConfigRunner,
        new WiseTimeConnectorHealthIndicator(wiseTimeConnector));

    healthCheckTimer = new Timer("health-check-timer", false);
    tagTimer = new Timer("tag-check-timer", true);
    managedConfigTimer = new Timer("manage-config-timer", true);

    // Connector is initially in stopped state
    connectorRun = new CompletableFuture<>();
    connectorRun.complete(null);
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
    if (!connectorRun.isDone()) {
      log.warn("Cannot start the connector while it is still running. Start attempt ignored.");
      return;
    }

    connectorRun = CompletableFuture
        .supplyAsync(() -> {
          // Init may take a while to complete if connector runs self checks
          wiseTimeConnector.init(connectorModule);

          try {
            timePoster.start();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          healthRunner.setShutdownFunction(() -> connectorRun.cancel(true));

          healthCheckTimer.scheduleAtFixedRate(
              healthRunner, TimeUnit.SECONDS.toMillis(5), TimeUnit.MINUTES.toMillis(1)
          );

          tagTimer.scheduleAtFixedRate(
              tagRunner, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5)
          );

          managedConfigTimer.scheduleAtFixedRate(
              managedConfigRunner, TimeUnit.SECONDS.toMillis(15), TimeUnit.MINUTES.toMillis(5)
          );

          while (timePoster.isRunning()) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              break;
            }
          }
          stop();
          return null;
        });

    // Block until stopped
    try {
      connectorRun.get();
    } catch (Exception e) {
      // Suppress errors during connector shutdown
      if (!connectorRun.isDone()) {
        throw e;
      }
    }
    log.info("Connector stopped");
  }

  @Override
  public void stop() {
    try {
      healthRunner.cancel();
      healthCheckTimer.cancel();
      healthCheckTimer.purge();

      managedConfigTimer.cancel();
      managedConfigTimer.purge();

      tagTimer.cancel();
      tagTimer.purge();
      timePoster.stop();

    } catch (Exception e) {
      log.warn("There was an error while stopping the connector", e);
    } finally {
      wiseTimeConnector.shutdown();
      connectorRun.cancel(true);
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

  private TimePoster createTimePoster(ConnectorControllerConfiguration configuration,
                                      ApiClient apiClient,
                                      SQLiteHelper sqLiteHelper,
                                      ConnectorInfoProvider connectorInfoProvider) {
    final ConnectorControllerBuilderImpl.PostedTimeLoadMode mode = configuration.getPostedTimeLoadMode();
    switch (mode) {
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
            metricService,
            sqLiteHelper);
      case DISABLED:
        return new NoOpTimePoster();
      default:
        log.error("unknown posted time fetch mode={}, defaulting to {}", mode, NoOpTimePoster.class.getName());
        return new NoOpTimePoster();
    }
  }

  private TagRunner createTagRunner(ConnectorControllerConfiguration configuration, WiseTimeConnector wiseTimeConnector) {
    switch (configuration.getTagScanMode()) {
      case ENABLED:
        return new TagRunner(wiseTimeConnector);
      case DISABLED:
        return new NoOpTagRunner();
      default:
        log.error("Unexpected tag runner mode {}. Fallback to ENABLED", configuration.getTagScanMode());
        return new TagRunner(wiseTimeConnector);
    }
  }
}
