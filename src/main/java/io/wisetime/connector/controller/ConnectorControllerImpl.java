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
import io.wisetime.connector.tag.NoOpTagSlowLoopRunner;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.tag.TagSlowLoopRunner;
import io.wisetime.connector.time_poster.NoOpTimePoster;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.long_polling.FetchClientTimePoster;
import io.wisetime.connector.time_poster.webhook.WebhookTimePoster;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Data;
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

  TimerTaskSchedule healthTaskSchedule = new TimerTaskSchedule(
      TimeUnit.SECONDS.toMillis(5), TimeUnit.MINUTES.toMillis(1));
  TimerTaskSchedule tagTaskSchedule = new TimerTaskSchedule(TimeUnit.SECONDS.toMillis(15),
      TimeUnit.MINUTES.toMillis(1));
  TimerTaskSchedule tagSlowLoopTaskSchedule = new TimerTaskSchedule(TimeUnit.SECONDS.toMillis(15),
      TimeUnit.MINUTES.toMillis(5));
  TimerTaskSchedule managedConfigTaskSchedule = new TimerTaskSchedule(TimeUnit.SECONDS.toMillis(15),
      TimeUnit.MINUTES.toMillis(5));

  private final AtomicReference<ExecutorService> connectorExecutor = new AtomicReference<>();
  private final TimePoster timePoster;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;

  private final TagRunner tagRunner;
  private final TagSlowLoopRunner tagSlowLoopRunner;
  private final HealthCheck healthRunner;
  private final ManagedConfigRunner managedConfigRunner;

  private final MetricService metricService;

  private final Timer healthCheckTimer;
  private final Timer tagTimer;
  private final Timer tagSlowLoopTimer;
  private final Timer managedConfigTimer;

  ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    healthRunner = new HealthCheck();
    metricService = new MetricService();
    wiseTimeConnector = new WiseTimeConnectorMetricWrapper(configuration.getWiseTimeConnector(), metricService);

    tagRunner = createTagRunner(configuration, wiseTimeConnector);
    tagSlowLoopRunner = createTagSlowLoopRunner(configuration, wiseTimeConnector);

    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);
    apiClient = new ApiClientTagWrapper(apiClient, tagRunner);

    final SQLiteHelper sqLiteHelper = new SQLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper),
        (int) TimeUnit.MILLISECONDS.toMinutes(tagSlowLoopTaskSchedule.getPeriodMs()));

    final ConnectorInfoProvider connectorInfoProvider = new ConstantConnectorInfoProvider();
    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper, connectorInfoProvider);

    managedConfigRunner = new ManagedConfigRunner(wiseTimeConnector, apiClient, connectorInfoProvider);

    healthRunner.addHealthIndicator(tagRunner,
        tagSlowLoopRunner,
        timePoster,
        managedConfigRunner,
        new WiseTimeConnectorHealthIndicator(wiseTimeConnector));

    healthCheckTimer = new Timer("health-check-timer", true);
    tagTimer = new Timer("tag-check-timer", true);
    tagSlowLoopTimer = new Timer("tag-slow-loop-timer", true);
    managedConfigTimer = new Timer("manage-config-timer", true);
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
    if (!connectorExecutor.compareAndSet(null, Executors.newScheduledThreadPool(0))) {
      log.warn("Cannot start the connector while it is still running. Start attempt ignored.");
      return;
    }

    // Init may take a while to complete if connector runs self checks
    wiseTimeConnector.init(connectorModule);

    try {
      timePoster.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    healthRunner.setShutdownFunction(this::stop);

    healthCheckTimer.scheduleAtFixedRate(healthRunner,
        healthTaskSchedule.getInitialDelayMs(), healthTaskSchedule.getPeriodMs());

    tagTimer.scheduleAtFixedRate(tagRunner,
        tagTaskSchedule.getInitialDelayMs(), tagTaskSchedule.getPeriodMs());

    tagSlowLoopTimer.scheduleAtFixedRate(tagSlowLoopRunner,
        tagSlowLoopTaskSchedule.getInitialDelayMs(), tagSlowLoopTaskSchedule.getPeriodMs());

    managedConfigTimer.scheduleAtFixedRate(managedConfigRunner,
        managedConfigTaskSchedule.getInitialDelayMs(), managedConfigTaskSchedule.getPeriodMs());

    connectorExecutor.get().awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    log.info("Connector stopped");
  }

  @Override
  public void stop() {
    log.info("Stopping connector");
    try {
      healthRunner.cancel();
      healthCheckTimer.cancel();
      healthCheckTimer.purge();

      managedConfigTimer.cancel();
      managedConfigTimer.purge();

      tagTimer.cancel();
      tagTimer.purge();

      tagSlowLoopTimer.cancel();
      tagSlowLoopTimer.purge();
      timePoster.stop();

      connectorExecutor.get().shutdownNow();
      if (!connectorExecutor.get().awaitTermination(60, TimeUnit.SECONDS)) {
        log.error("Failed to gracefully stop connector. Halting process now");
        System.exit(-1);
      }

    } catch (Exception e) {
      log.warn("There was an error while stopping the connector", e);
    } finally {
      connectorExecutor.set(null);
      wiseTimeConnector.shutdown();
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
            connectorExecutor::get,
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

  private TagSlowLoopRunner createTagSlowLoopRunner(ConnectorControllerConfiguration configuration,
                                                    WiseTimeConnector wiseTimeConnector) {
    switch (configuration.getTagScanMode()) {
      case ENABLED:
        return new TagSlowLoopRunner(wiseTimeConnector);
      case DISABLED:
        return new NoOpTagSlowLoopRunner();
      default:
        log.error("Unexpected tag runner mode {}. Fallback to ENABLED", configuration.getTagScanMode());
        return new TagSlowLoopRunner(wiseTimeConnector);
    }
  }

  @Data
  @AllArgsConstructor
  static class TimerTaskSchedule {

    private long initialDelayMs;
    private long periodMs;
  }
}
