/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.fetch_client;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import io.wisetime.connector.TimePosterRunner;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.generated.connect.TimeGroup;

import static io.wisetime.connector.fetch_client.TimeGroupIdStore.IN_PROGRESS;
import static io.wisetime.connector.fetch_client.TimeGroupIdStore.PERMANENT_FAILURE_AND_SENT;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
public class FetchClient implements Runnable, TimePosterRunner {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private static final Logger log = LoggerFactory.getLogger(FetchClient.class);

  private final AtomicReference<DateTime> lastSuccessfulRun = new AtomicReference<>(DateTime.now());

  private final FetchClientSpec clientSpec;
  private final ExecutorService postTimeExecutor;
  private final TimeGroupStatusUpdater timeGroupStatusUpdater;

  private ExecutorService fetchClientExecutor = null;

  public FetchClient(FetchClientSpec clientSpec) {
    this.clientSpec = clientSpec;
    /*
       The thread pool processes each time row that is returned from the batch fetch.
       Up to a maximum of three concurrent posts are permitted.
     */
    final int threadPoolSize = Math.min(clientSpec.getLimit(), 3);
    this.postTimeExecutor = Executors.newFixedThreadPool(threadPoolSize);
    timeGroupStatusUpdater = new TimeGroupStatusUpdater(clientSpec.getTimeGroupIdStore(), clientSpec.getApiClient());
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final List<TimeGroup> fetchedTimeGroups = clientSpec.getApiClient().fetchTimeGroups(clientSpec.getLimit());

        for (TimeGroup timeGroup : fetchedTimeGroups) {
          if (!timeGroupAlreadyProcessed(timeGroup)) {
            // save the rows to the DB synchronously
            clientSpec.getTimeGroupIdStore().putTimeGroupId(timeGroup.getGroupId(), IN_PROGRESS, "");

            // trigger async process to post to external system
            postTimeExecutor.submit(() -> {
              PostResult result;
              try {
                result = clientSpec.getConnector().postTime(null, timeGroup);
              } catch (Exception e) {
                // We can't rule out postTime throws runtime exceptions, in this case permanently fail the time group:
                // most likely a bug
                result = PostResult.PERMANENT_FAILURE.withError(e).withMessage(e.getMessage());
                log.error("Unexpected exception while trying to post time", e);
              }
              clientSpec.getTimeGroupIdStore()
                  .putTimeGroupId(timeGroup.getGroupId(), result.name(), result.getMessage().orElse(""));
              timeGroupStatusUpdater.processSingle(timeGroup.getGroupId(), result);
            });
          }
        }
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Error while fetching new time group", e);
      }
    }
  }

  private boolean timeGroupAlreadyProcessed(TimeGroup timeGroup) {
    Optional<String> timeGroupStatus = clientSpec.getTimeGroupIdStore().alreadySeen(timeGroup.getGroupId());
    // For any failure state: Allow reprocessing. For SUCCESS and IN_PROGRESS deny reprocessing
    return timeGroupStatus.filter(s ->
        !PostResult.TRANSIENT_FAILURE.name().equals(s)
            && !PostResult.PERMANENT_FAILURE.name().equals(s)
            && !PERMANENT_FAILURE_AND_SENT.equals(s)).isPresent();
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
    return DateTime.now().minusMinutes(MAX_MINS_SINCE_SUCCESS).isBefore(lastSuccessfulRun.get())
        && timeGroupStatusUpdater.isHealthy();
  }

  @Override
  public boolean isRunning() {
    return fetchClientExecutor != null && !fetchClientExecutor.isShutdown();
  }
}
