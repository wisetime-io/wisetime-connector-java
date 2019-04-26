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
import io.wisetime.generated.connect.TimeGroupStatus;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
public class FetchClient implements Runnable, TimePosterRunner {

  private static final String IN_PROGRESS = "IN_PROGRESS";
  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private static final Logger log = LoggerFactory.getLogger(FetchClient.class);

  private final AtomicReference<DateTime> lastSuccessfulRun = new AtomicReference<>(DateTime.now());

  private final FetchClientSpec clientSpec;
  private final ExecutorService postTimeExecutor;

  private ExecutorService fetchClientExecutor = null;

  public FetchClient(FetchClientSpec clientSpec) {
    this.clientSpec = clientSpec;
    /*
       The thread pool processes each time row that is returned from the batch fetch.
       Up to a maximum of three concurrent posts are permitted.
     */
    final int threadPoolSize = Math.min(clientSpec.getLimit(), 3);
    this.postTimeExecutor = Executors.newFixedThreadPool(threadPoolSize);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        final List<TimeGroup> fetchedTimeGroups = clientSpec.getApiClient().fetchTimeGroups(
            clientSpec.getFetchClientId(),
            clientSpec.getLimit());

        for (TimeGroup timeGroup : fetchedTimeGroups) {
          // save the rows to the DB synchronously
          clientSpec.getTimeGroupIdStore().putTimeGroupId(timeGroup.getGroupId(), IN_PROGRESS);

          // trigger async process to post to external system
          postTimeExecutor.submit(() -> {
            if (!timeGroupAlreadyProcessed(timeGroup)) {
              PostResult result = clientSpec.getConnector().postTime(null, timeGroup);
              clientSpec.getTimeGroupIdStore().putTimeGroupId(timeGroup.getGroupId(), result.name());
              updateTimeGroupStatus(timeGroup, result);
            }
          });
        }

        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Error while fetching new time group for fetch client id: " + clientSpec.getFetchClientId(), e);
      }
    }
  }

  private boolean timeGroupAlreadyProcessed(TimeGroup timeGroup) {
    Optional<String> timeGroupStatus = clientSpec.getTimeGroupIdStore().alreadySeen(timeGroup.getGroupId());
    if (!timeGroupStatus.isPresent()) {
      return false;
    }
    if (IN_PROGRESS.equals(timeGroupStatus.get())) {
      return true;
    }
    PostResult postResult = PostResult.valueOf(timeGroupStatus.get());
    if (postResult == PostResult.SUCCESS || postResult == PostResult.PERMANENT_FAILURE) {
      // Successfully or permanently failed processed group -> try updating status again
      updateTimeGroupStatus(timeGroup, postResult);
      return true;
    }
    // recorded transient failure state: process time group
    return false;
  }

  private void updateTimeGroupStatus(TimeGroup timeGroup, PostResult result) {
    try {
      // TRANSIENT_FAILURE doesn't need to be handled.
      // simply sending no update is considered a transient failure after a certain timeout
      switch (result) {
        case SUCCESS:
          clientSpec.getApiClient().updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.SUCCESS)
              .timeGroupId(timeGroup.getGroupId())
              .fetchClientId(clientSpec.getFetchClientId()));
          break;
        case PERMANENT_FAILURE:
          clientSpec.getApiClient().updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.FAILURE)
              .timeGroupId(timeGroup.getGroupId())
              .fetchClientId(clientSpec.getFetchClientId())
              .message(result.getMessage().orElse("Unexpected error while posting time")));
          break;
        default:
          // do nothing
          log.debug("TRANSIENT_FAILURE for time group");
      }
    } catch (Exception e) {
      log.error("Error while updating posted time status.", e);
    }
  }

  public void start() {
    if (fetchClientExecutor != null) {
      throw new IllegalStateException("Fetch Client already running");
    }
    fetchClientExecutor = Executors.newSingleThreadExecutor();
    fetchClientExecutor.submit(this);
  }

  public void stop() {
    fetchClientExecutor.shutdownNow();
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
