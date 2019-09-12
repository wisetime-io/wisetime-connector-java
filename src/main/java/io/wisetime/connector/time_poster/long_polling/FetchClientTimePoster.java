/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.generated.connect.TimeGroup;
import lombok.extern.slf4j.Slf4j;

import static io.wisetime.connector.time_poster.long_polling.TimeGroupIdStore.IN_PROGRESS;
import static io.wisetime.connector.time_poster.long_polling.TimeGroupIdStore.SUCCESS_AND_SENT;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
@Slf4j
public class FetchClientTimePoster implements Runnable, TimePoster {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private final AtomicReference<DateTime> lastSuccessfulRun = new AtomicReference<>(DateTime.now());

  private final ExecutorService postTimeExecutor;
  private final TimeGroupStatusUpdater timeGroupStatusUpdater;
  private final TimeGroupIdStore timeGroupIdStore;
  private final int timeGroupsFetchLimit;
  private final ApiClient apiClient;
  private final WiseTimeConnector wiseTimeConnector;

  private final RetryPolicy retryPolicy;

  private ExecutorService fetchClientExecutor = null;

  @SuppressWarnings("ParameterNumber")
  public FetchClientTimePoster(WiseTimeConnector wiseTimeConnector, ApiClient apiClient, HealthCheck healthCheck,
                               SQLiteHelper sqLiteHelper, int timeGroupsFetchLimit) {
    this(wiseTimeConnector, apiClient, healthCheck, new TimeGroupIdStore(sqLiteHelper),
        timeGroupsFetchLimit);
  }

  @SuppressWarnings("ParameterNumber")
  public FetchClientTimePoster(WiseTimeConnector wiseTimeConnector, ApiClient apiClient, HealthCheck healthCheck,
                               TimeGroupIdStore timeGroupIdStore, int timeGroupsFetchLimit) {
    this.wiseTimeConnector = wiseTimeConnector;
    this.apiClient = apiClient;
    this.timeGroupIdStore = timeGroupIdStore;
    this.timeGroupsFetchLimit = timeGroupsFetchLimit;
    /*
       The thread pool processes each time row that is returned from the batch fetch.
       Only one concurrent post is allowed until WiseTimeConnector#postTime is guaranteed to be thread safe.
     */
    this.postTimeExecutor = Executors.newSingleThreadExecutor();
    timeGroupStatusUpdater = new TimeGroupStatusUpdater(timeGroupIdStore, apiClient);
    healthCheck.addHealthIndicator(timeGroupStatusUpdater);

    // retry on all exceptions that either indicate failed http status or general connectivity issue
    // exponential backoff starting with 10 seconds max 600 seconds
    // after 10 retries we consider it terminally failed.
    // the health check for the connector will have failed at that point anyways, so it should already be shutting down
    retryPolicy = new RetryPolicy()
        .retryOn(IOException.class)
        .withBackoff(10, 600, TimeUnit.SECONDS)
        .withMaxRetries(10);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final List<TimeGroup> fetchedTimeGroups = Failsafe.with(retryPolicy)
            .get(() -> apiClient.fetchTimeGroups(timeGroupsFetchLimit));
        log.debug("Received {} for time posting", fetchedTimeGroups);

        for (TimeGroup timeGroup : fetchedTimeGroups) {
          if (!timeGroupAlreadyProcessed(timeGroup)) {
            log.debug("Processing time group: {}", timeGroup);
            // save the rows to the DB synchronously
            timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), IN_PROGRESS, "");

            // trigger async process to post to external system
            postTimeExecutor.submit(() -> {
              PostResult result;
              try {
                result = wiseTimeConnector.postTime(null, timeGroup);
              } catch (Exception e) {
                // We can't rule out postTime throws runtime exceptions, in this case permanently fail the time group:
                // most likely a bug
                result = PostResult.PERMANENT_FAILURE().withError(e).withMessage(e.getMessage());
                log.error("Unexpected exception while trying to post time", e);
              }
              timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), result.name(), result.getMessage().orElse(""));
              timeGroupStatusUpdater.processSingle(timeGroup.getGroupId(), result);
            });
          }
        }
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Error while fetching new time groups", e);
      }
    }
  }

  private boolean timeGroupAlreadyProcessed(TimeGroup timeGroup) {
    Optional<String> timeGroupStatus = timeGroupIdStore.alreadySeen(timeGroup.getGroupId());
    // For any failure state: Allow reprocessing. For SUCCESS and IN_PROGRESS deny reprocessing
    return timeGroupStatus.filter(status ->
        PostResultStatus.SUCCESS.name().equals(status)
            || SUCCESS_AND_SENT.equals(status)
            || IN_PROGRESS.equals(status))
        .isPresent();
  }

  public void start() {
    if (fetchClientExecutor != null) {
      throw new IllegalStateException("Fetch Client already running");
    }
    fetchClientExecutor = Executors.newSingleThreadExecutor();
    fetchClientExecutor.submit(this);
    timeGroupStatusUpdater.startScheduler();
  }

  public void stop() {
    fetchClientExecutor.shutdownNow();
    timeGroupStatusUpdater.stopScheduler();
    fetchClientExecutor = null;
  }

  @Override
  public boolean isHealthy() {
    return DateTime.now().minusMinutes(MAX_MINS_SINCE_SUCCESS).isBefore(lastSuccessfulRun.get());
  }

  @Override
  public boolean isRunning() {
    return fetchClientExecutor != null && !fetchClientExecutor.isShutdown();
  }
}
