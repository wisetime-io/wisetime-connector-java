/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.fetch_client;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import io.wisetime.connector.TimePosterRunner;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.generated.connect.TimeGroupStatus;

/**
 * Implements a fetch based approach to retrieve time groups.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
public class FetchClient implements Runnable, TimePosterRunner {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;

  private static final Logger log = LoggerFactory.getLogger(FetchClient.class);

  private ApiClient apiClient;

  private TimeGroupIdStore timeGroupIdStore;

  private String fetchClientId;

  private int limit;

  private WiseTimeConnector connector;

  private ExecutorService postTimeExecutor;

  private final AtomicReference<DateTime> lastSuccessfulRun;

  private ExecutorService fetchClientExecutor;

  public FetchClient(ApiClient apiClient,
                     WiseTimeConnector connector,
                     TimeGroupIdStore timeGroupIdStore,
                     String fetchClientId,
                     int limit) {
    this.apiClient = apiClient;
    this.connector = connector;
    this.timeGroupIdStore = timeGroupIdStore;
    this.fetchClientId = fetchClientId;
    this.limit = limit;
    this.postTimeExecutor = Executors.newFixedThreadPool(limit);
    this.lastSuccessfulRun = new AtomicReference<>(DateTime.now());
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        apiClient.fetchTimeGroups(fetchClientId, limit).forEach(timeGroup ->
            postTimeExecutor.submit(() -> {
              if (!timeGroupIdStore.alreadySeen(timeGroup.getGroupId())) {
                timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId());
                PostResult result = connector.postTime(null, timeGroup);
                try {
                  // TRANSIENT_FAILURE doesn't need to be handled.
                  // simply sending no update is considered a transient failure after a certain timeout
                  switch (result) {
                    case SUCCESS:
                      apiClient.updatePostedTimeStatus(new TimeGroupStatus()
                          .status(TimeGroupStatus.StatusEnum.SUCCESS)
                          .timeGroupId(timeGroup.getGroupId())
                          .fetchClientId(fetchClientId));
                      break;
                    case PERMANENT_FAILURE:
                      apiClient.updatePostedTimeStatus(new TimeGroupStatus()
                          .status(TimeGroupStatus.StatusEnum.FAILURE)
                          .timeGroupId(timeGroup.getGroupId())
                          .fetchClientId(fetchClientId)
                          .message(result.getMessage().orElse("Unexpected error while posting time")));
                      break;
                    default:
                      // do nothing
                      log.debug("TRANSIENT_FAILURE for time group");
                  }
                } catch (Exception e) {
                  log.error("Error while updating posted time status.", e);
                }
                // At this point we either accepted or failed the time group.
                // In case of success WiseTime will never send it again
                // In case of Failure: we want to be able to process it again
                timeGroupIdStore.deleteTimeGroupId(timeGroup.getGroupId());
              }
            })
        );
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Error while fetching new time group for fetch client id: " + fetchClientId, e);
      }
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
