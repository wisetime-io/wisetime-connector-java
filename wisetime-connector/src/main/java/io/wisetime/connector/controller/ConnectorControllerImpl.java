/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.ConnectorModule;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.activity_type.ActivityTypeRunner;
import io.wisetime.connector.activity_type.ActivityTypeSlowLoopRunner;
import io.wisetime.connector.activity_type.NoOpActivityTypeRunner;
import io.wisetime.connector.activity_type.NoOpActivityTypeSlowLoopRunner;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.config.ManagedConfigRunner;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.connector.config.info.ConstantConnectorInfoProvider;
import io.wisetime.connector.datastore.FileStore;
import io.wisetime.connector.datastore.SqLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.connector.metric.ApiClientMetricWrapper;
import io.wisetime.connector.metric.MetricInfo;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.connector.tag.ApiClientTagWrapper;
import io.wisetime.connector.tag.NoOpTagRunner;
import io.wisetime.connector.tag.NoOpTagSlowLoopRunner;
import io.wisetime.connector.tag.TagRunner;
import io.wisetime.connector.tag.TagSlowLoopRunner;
import io.wisetime.connector.time_poster.NoOpTimePoster;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.long_polling.FetchClientTimePoster;
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


  private final AtomicReference<ExecutorService> connectorExecutor = new AtomicReference<>();
  private final TimePoster timePoster;
  private final WiseTimeConnector wiseTimeConnector;
  private final ConnectorModule connectorModule;

  private final TagRunner tagRunner;
  private final TagSlowLoopRunner tagSlowLoopRunner;
  private final ActivityTypeRunner activityTypeRunner;
  private final ActivityTypeSlowLoopRunner activityTypeSlowLoopRunner;
  private final HealthCheck healthRunner;
  private final ManagedConfigRunner managedConfigRunner;

  private final MetricService metricService;

  private final Timer healthCheckTimer;
  private final Timer tagTimer;
  private final Timer tagSlowLoopTimer;
  private final Timer activityTypeTimer;
  private final Timer activityTypeSlowLoopTimer;
  private final Timer managedConfigTimer;

  private final TimerTaskSchedule tagSlowLoopTaskSchedule;
  private final TimerTaskSchedule activityTypeSlowLoopTaskSchedule;

  private final TimerTaskSchedule healthTaskSchedule;

  ConnectorControllerImpl(ConnectorControllerConfiguration configuration) {
    metricService = new MetricService();
    wiseTimeConnector = configuration.getWiseTimeConnector();
    tagSlowLoopTaskSchedule = new TimerTaskSchedule(
        TimeUnit.SECONDS.toMillis(15),
        TimeUnit.MINUTES.toMillis(5));
    activityTypeSlowLoopTaskSchedule = new TimerTaskSchedule(
        TimeUnit.MINUTES.toMillis(1),
        TimeUnit.MINUTES.toMillis(15));

    healthTaskSchedule = new TimerTaskSchedule(
        TimeUnit.SECONDS.toMillis(5),
        TimeUnit.MINUTES.toMillis(1)
    );

    tagRunner = createTagRunner(configuration, wiseTimeConnector);
    tagSlowLoopRunner = createTagSlowLoopRunner(configuration, wiseTimeConnector);

    activityTypeRunner = createActivityTypeRunner(configuration, wiseTimeConnector);
    activityTypeSlowLoopRunner = createActivityTypeSlowLoopRunner(configuration, wiseTimeConnector);

    ApiClient apiClient = new ApiClientMetricWrapper(configuration.getApiClient(), metricService);
    apiClient = new ApiClientTagWrapper(apiClient, tagRunner);
    healthRunner = new HealthCheck(apiClient, wiseTimeConnector);

    final SqLiteHelper sqLiteHelper = new SqLiteHelper(configuration.isForcePersistentStorage());
    connectorModule = new ConnectorModule(apiClient, new FileStore(sqLiteHelper),
        new ConnectorModule.IntervalConfig()
            .setActivityTypeSlowLoopIntervalMinutes(
                (int) MILLISECONDS.toMinutes(activityTypeSlowLoopTaskSchedule.getPeriodMs()))
            .setTagSlowLoopIntervalMinutes(
                (int) MILLISECONDS.toMinutes(tagSlowLoopTaskSchedule.getPeriodMs()))
    );

    final ConnectorInfoProvider connectorInfoProvider = new ConstantConnectorInfoProvider();
    timePoster = createTimePoster(configuration, apiClient, sqLiteHelper);

    managedConfigRunner = new ManagedConfigRunner(wiseTimeConnector, apiClient, connectorInfoProvider);

    healthRunner.addHealthIndicator(tagRunner,
        tagSlowLoopRunner,
        activityTypeRunner,
        activityTypeSlowLoopRunner,
        timePoster,
        managedConfigRunner);

    healthCheckTimer = new Timer("health-check-timer", true);
    tagTimer = new Timer("tag-check-timer", true);
    tagSlowLoopTimer = new Timer("tag-slow-loop-timer", true);
    activityTypeTimer = new Timer("activity-type-timer", true);
    activityTypeSlowLoopTimer = new Timer("activity-type-slow-loop-timer", true);
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
    if (!connectorExecutor.compareAndSet(null, Executors.newCachedThreadPool())) {
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

    TimerTaskSchedule tagTaskSchedule = new TimerTaskSchedule(
        TimeUnit.SECONDS.toMillis(15),
        TimeUnit.MINUTES.toMillis(getTagTaskScheduleMins()));
    tagTimer.scheduleAtFixedRate(tagRunner,
        tagTaskSchedule.getInitialDelayMs(), tagTaskSchedule.getPeriodMs());

    tagSlowLoopTimer.scheduleAtFixedRate(tagSlowLoopRunner,
        tagSlowLoopTaskSchedule.getInitialDelayMs(), tagSlowLoopTaskSchedule.getPeriodMs());

    TimerTaskSchedule activityTypeTaskSchedule = new TimerTaskSchedule(
        TimeUnit.SECONDS.toMillis(15),
        TimeUnit.MINUTES.toMillis(getActivityTypeTaskScheduleMins()));
    activityTypeTimer.scheduleAtFixedRate(activityTypeRunner,
        activityTypeTaskSchedule.getInitialDelayMs(), activityTypeTaskSchedule.getPeriodMs());

    TimerTaskSchedule managedConfigTaskSchedule = new TimerTaskSchedule(
        TimeUnit.SECONDS.toMillis(15),
        TimeUnit.MINUTES.toMillis(5));

    activityTypeSlowLoopTimer.scheduleAtFixedRate(activityTypeSlowLoopRunner,
        activityTypeSlowLoopTaskSchedule.getInitialDelayMs(), activityTypeSlowLoopTaskSchedule.getPeriodMs());

    managedConfigTimer.scheduleAtFixedRate(managedConfigRunner,
        managedConfigTaskSchedule.getInitialDelayMs(), managedConfigTaskSchedule.getPeriodMs());

    final boolean terminateSuccess = connectorExecutor.get().awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    log.info("Connector stopped, graceful termination: {}", terminateSuccess);
  }

  int getActivityTypeTaskScheduleMins() {
    return 5;
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

      activityTypeTimer.cancel();
      activityTypeTimer.purge();

      activityTypeSlowLoopTimer.cancel();
      activityTypeSlowLoopTimer.purge();

      connectorExecutor.get().shutdownNow();
      if (!connectorExecutor.get().awaitTermination(60, TimeUnit.SECONDS)) {
        log.error("Failed to gracefully stop connector. Halting process now");
        System.exit(-1);
      }

      connectorModule.getApiClient().shutdown();

    } catch (Exception e) {
      log.warn("There was an error while stopping the connector", e);
    } finally {
      connectorExecutor.set(null);
      wiseTimeConnector.shutdown();
    }
  }

  @Override
  public boolean isHealthy() {
    return healthRunner.checkBaseLibraryHealth();
  }

  @Override
  public MetricInfo getMetrics() {
    return metricService.getMetrics();
  }

  @Override
  public ConnectorModule getConnectorModule() {
    return connectorModule;
  }

  private TimePoster createTimePoster(ConnectorControllerConfiguration configuration,
      ApiClient apiClient,
      SqLiteHelper sqLiteHelper) {
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
      case DISABLED:
        return new NoOpTimePoster();
      default:
        log.error("unknown posted time fetch mode={}, defaulting to {}", mode, NoOpTimePoster.class.getName());
        return new NoOpTimePoster();
    }
  }

  private TagRunner createTagRunner(ConnectorControllerConfiguration configuration,
      WiseTimeConnector wiseTimeConnector) {
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

  private ActivityTypeRunner createActivityTypeRunner(ConnectorControllerConfiguration configuration,
      WiseTimeConnector wiseTimeConnector) {
    switch (configuration.getActivityTypeScanMode()) {
      case ENABLED:
        return new ActivityTypeRunner(wiseTimeConnector);
      case DISABLED:
        return new NoOpActivityTypeRunner();
      default:
        log.error("Unexpected activity type runner mode {}. Fallback to ENABLED", configuration.getTagScanMode());
        return new ActivityTypeRunner(wiseTimeConnector);
    }
  }

  private ActivityTypeSlowLoopRunner createActivityTypeSlowLoopRunner(ConnectorControllerConfiguration configuration,
      WiseTimeConnector wiseTimeConnector) {
    switch (configuration.getActivityTypeScanMode()) {
      case ENABLED:
        return new ActivityTypeSlowLoopRunner(wiseTimeConnector);
      case DISABLED:
        return new NoOpActivityTypeSlowLoopRunner();
      default:
        log.error("Unexpected activity type runner mode {}. Fallback to ENABLED", configuration.getTagScanMode());
        return new ActivityTypeSlowLoopRunner(wiseTimeConnector);
    }
  }

  long getTagTaskScheduleMins() {
    return 1;
  }

  TimerTaskSchedule getTagSlowLoopTaskSchedule() {
    return tagSlowLoopTaskSchedule;
  }

  TimerTaskSchedule getActivityTypeSlowLoopTaskSchedule() {
    return activityTypeSlowLoopTaskSchedule;
  }

  TimerTaskSchedule getHealthTaskSchedule() {
    return healthTaskSchedule;
  }

  @Data
  @AllArgsConstructor
  static class TimerTaskSchedule {

    private long initialDelayMs;
    private long periodMs;
  }
}
