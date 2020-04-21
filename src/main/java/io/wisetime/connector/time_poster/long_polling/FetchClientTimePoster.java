/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.IN_PROGRESS;
import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.SUCCESS_AND_SENT;
import static java.lang.Thread.sleep;

import com.google.common.collect.Sets;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.datastore.SQLiteHelper;
import io.wisetime.connector.health.HealthCheck;
import io.wisetime.connector.time_poster.TimePoster;
import io.wisetime.connector.time_poster.deduplication.TimeGroupId;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.generated.connect.TimeGroup;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.joda.time.DateTime;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
@Slf4j
public class FetchClientTimePoster implements Runnable, TimePoster {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private final AtomicReference<DateTime> lastSuccessfulRun = new AtomicReference<>(DateTime.now());

  private final ThreadPoolExecutor postTimeExecutor;
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
    this.postTimeExecutor = (ThreadPoolExecutor) Executors.newSingleThreadExecutor();
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

        final ResultPair resultPair = splitResults(fetchedTimeGroups);

        for (TimeGroup timeGroup : resultPair.getCompletedList()) {
          // if SUCCESS or SUCCESS_SEND, send the status update to connect-api-server (async)
          timeGroupStatusUpdater.processSingle(timeGroup.getGroupId(), PostResult.SUCCESS());
        }

        final Function<Optional<String>, Boolean> isProcessedFn = (statusOpt) -> statusOpt.map(status ->
            PostResultStatus.SUCCESS.name().equals(status)
                || SUCCESS_AND_SENT.equals(status)
                || IN_PROGRESS.equals(status))
            .orElse(false);

        for (TimeGroup timeGroup : resultPair.getPendingList()) {
          Optional<String> timeGroupStatus = timeGroupIdStore.alreadySeenFetchClient(timeGroup.getGroupId());

          if (!isProcessedFn.apply(timeGroupStatus)) {
            // isProcessedFn will skip anything with state `IN_PROGRESS`, SUCCESS or SUCCESS_AND_SENT
            log.debug("Processing time group: {}", timeGroup);

            // save the rows to the DB synchronously as IN_PROGRESS
            timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), IN_PROGRESS, "");

            if (postTimeExecutor.isShutdown()) {
              // check shutdown state before queueing
              return;
            }

            // trigger async process to post to external system
            postTimeExecutor.submit(() -> {
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
                log.info("Prevented reprocessing of time group: {}. It is already in terminal state: {}",
                    timeGroup.getGroupId(), status.get());
                return;
              }
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

        pauseForPostExecutor();

        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Error while fetching new time groups", e);
      }
    }
  }

  private void pauseForPostExecutor() {
    long endWait = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(4);
    while (!fetchClientExecutor.isShutdown() && !postTimeExecutor.isShutdown()
     && (postTimeExecutor.getActiveCount() + postTimeExecutor.getQueue().size()) > 0
     && endWait > System.currentTimeMillis()) {
      if (log.isTraceEnabled()) {
        log.trace("allowing time for typical post to occur");
      }
    }
  }

  ResultPair splitResults(List<TimeGroup> fetchedTimeGroups) {
    final Map<TimeGroupId, String> postResultCacheMap = timeGroupIdStore.fetchPostResultList(
        fetchedTimeGroups.stream()
            .map(timeGroup -> TimeGroupId.create(timeGroup.getGroupId()))
            .collect(Collectors.toList()));

    Set<String> successStatusSet = Sets.newHashSet(PostResultStatus.SUCCESS.name(), SUCCESS_AND_SENT);

    final ResultPair resultPair = new ResultPair();

    for (TimeGroup timeGroup : fetchedTimeGroups) {
      if (postResultCacheMap.containsKey(TimeGroupId.create(timeGroup.getGroupId())) &&
          successStatusSet.contains(postResultCacheMap.get(TimeGroupId.create(timeGroup.getGroupId())))) {
        resultPair.getCompletedList().add(timeGroup);
      } else {
        resultPair.getPendingList().add(timeGroup);
      }
    }
    return resultPair;
  }

  @ToString
  @EqualsAndHashCode
  @Getter
  class ResultPair {
    private final List<TimeGroup> completedList = new ArrayList<>();
    private final List<TimeGroup> pendingList = new ArrayList<>();
  }

  public void start() {
    if (fetchClientExecutor != null && !fetchClientExecutor.isShutdown()) {
      throw new IllegalStateException("Fetch Client already running");
    }
    fetchClientExecutor = Executors.newSingleThreadExecutor();
    fetchClientExecutor.submit(this);
    timeGroupStatusUpdater.startScheduler();
  }

  public void stop() {
    fetchClientExecutor.shutdownNow();
    timeGroupStatusUpdater.stopScheduler();
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
