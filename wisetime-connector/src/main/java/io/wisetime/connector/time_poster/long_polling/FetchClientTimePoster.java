/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.IN_PROGRESS;
import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.SUCCESS_AND_SENT;

import com.google.common.annotations.VisibleForTesting;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.datastore.SqLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroup;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Slf4j
public class FetchClientTimePoster implements Runnable, TimePoster {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private final AtomicReference<DateTime> lastSuccessfulRun = new AtomicReference<>(DateTime.now());

  /**
   * The thread pool processes each time row that is returned from the batch fetch.
   * Only one concurrent post is allowed until WiseTimeConnector#postTime is guaranteed to be thread safe.
   */
  private final TimeGroupStatusUpdater timeGroupStatusUpdater;
  private final TimeGroupIdStore timeGroupIdStore;
  private final int timeGroupsFetchLimit;
  private final ApiClient apiClient;
  private final WiseTimeConnector wiseTimeConnector;
  private final Supplier<ExecutorService> executorProvider;

  @SuppressWarnings("ParameterNumber")
  public FetchClientTimePoster(WiseTimeConnector wiseTimeConnector, ApiClient apiClient, HealthCheck healthCheck,
      Supplier<ExecutorService> executorProvider, SqLiteHelper sqLiteHelper, int timeGroupsFetchLimit) {
    this(wiseTimeConnector, apiClient, healthCheck, executorProvider, new TimeGroupIdStore(sqLiteHelper),
        timeGroupsFetchLimit);
  }

  @SuppressWarnings("ParameterNumber")
  public FetchClientTimePoster(WiseTimeConnector wiseTimeConnector, ApiClient apiClient, HealthCheck healthCheck,
      Supplier<ExecutorService> executorProvider, TimeGroupIdStore timeGroupIdStore, int timeGroupsFetchLimit) {
    this.wiseTimeConnector = wiseTimeConnector;
    this.apiClient = apiClient;
    this.timeGroupIdStore = timeGroupIdStore;
    this.timeGroupsFetchLimit = timeGroupsFetchLimit;
    this.executorProvider = executorProvider;
    timeGroupStatusUpdater = new TimeGroupStatusUpdater(timeGroupIdStore, apiClient, executorProvider);
    healthCheck.addHealthIndicator(timeGroupStatusUpdater);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final List<TimeGroup> fetchedTimeGroups = apiClient.fetchTimeGroups(timeGroupsFetchLimit);
        processTimeGroups(fetchedTimeGroups);
      } catch (SocketTimeoutException e) {
        log.debug("Long polling timeout, reconnecting", e);
        try {
          // random delay 500 to 1000 ms
          Thread.sleep((long) (Math.random() * 500 + 500));
        } catch (InterruptedException ex) {
          log.warn("Interrupted during backoff sleep", e);
          return;
        }
      } catch (Exception e) {
        log.error("Error while fetching new time groups", e);
        try {
          Thread.sleep((long) (TimeUnit.SECONDS.toMillis(10) + Math.random() * 1000));
        } catch (InterruptedException ex) {
          log.warn("Interrupted during backoff sleep", e);
          return;
        }
      }
    }
  }

  @VisibleForTesting
  void processTimeGroups(List<TimeGroup> fetchedTimeGroups) {
    log.debug("Received {} for time posting", fetchedTimeGroups);
    for (TimeGroup timeGroup : fetchedTimeGroups) {
      Optional<String> timeGroupStatus = timeGroupIdStore.alreadySeenFetchClient(timeGroup.getGroupId());

      if (!skip(timeGroupStatus)) {
        // skip will skip anything with state `IN_PROGRESS`, SUCCESS or SUCCESS_AND_SENT
        log.debug("Processing time group: {}", timeGroup);

        // save the rows to the DB synchronously as IN_PROGRESS
        timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), IN_PROGRESS, "");

        postTime(timeGroup);
      } else if (timeGroupStatus.map(this::resendSuccessMessage).orElse(false)) {
        timeGroupStatusUpdater.processSingle(timeGroup.getGroupId(), PostResult.SUCCESS());
      }
    }
    lastSuccessfulRun.set(DateTime.now());
  }

  private void postTime(TimeGroup timeGroup) {
    // verify that the time group is not in an already processed state (i.e. not in IN_PROGRESS)
    // if it is we encountered a corner case where we rescheduled a time group because the
    // last try to process it stuck for too long in processing but completes successfully.
    // This is safe because processing time groups is done sequentially by a single thread
    // when we encounter the IN_PROGRESS state here it means it wasn't processed before
    Optional<String> status = timeGroupIdStore.getPostStatusForFetchClient(timeGroup.getGroupId());
    if (!status.isPresent()) {
      log.info("Encountered time group with no associated status: {}. "
              + "Cancel processing and waiting for retry",
          timeGroup.getGroupId());
      return;
    }
    if (!IN_PROGRESS.equals(status.get())) {
      log.info("Skip posting time group group: {}. The connector store sees this time group as having status: {}.",
          timeGroup.getGroupId(), status.get());
      return;
    }
    PostResult result;
    try {
      result = wiseTimeConnector.postTime(timeGroup);
    } catch (Exception e) {
      // We can't rule out postTime throws runtime exceptions, in this case permanently fail the time group:
      // most likely a bug
      result = PostResult.PERMANENT_FAILURE().withError(e).withMessage(e.getMessage());
      log.error("Unexpected exception while trying to post time {}", e.getMessage(), e);
    }
    timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), result.name(), result.getMessage().orElse(""));
    timeGroupStatusUpdater.processSingle(timeGroup.getGroupId(), result);
  }

  private boolean skip(Optional<String> status) {
    return status.map(s ->
        PostResultStatus.SUCCESS.name().equals(s)
            || SUCCESS_AND_SENT.equals(s)
            || IN_PROGRESS.equals(s))
        .orElse(false);
  }

  private boolean resendSuccessMessage(String status) {
    // If we got an already successful time group: Immediately send the status update to connect-api-server
    return PostResultStatus.SUCCESS.name().equals(status)
        || SUCCESS_AND_SENT.equals(status);
  }

  public void start() {
    executorProvider.get().submit(this);
    timeGroupStatusUpdater.startScheduler();
  }

  public void stop() {
    timeGroupStatusUpdater.stopScheduler();
  }

  @Override
  public boolean isHealthy() {
    return DateTime.now().minusMinutes(MAX_MINS_SINCE_SUCCESS).isBefore(lastSuccessfulRun.get());
  }

}
