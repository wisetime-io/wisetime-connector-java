/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.health.HealthIndicator;
import io.wisetime.generated.connect.TimeGroupStatus;
import lombok.extern.slf4j.Slf4j;

import static io.wisetime.connector.time_poster.long_polling.TimeGroupIdStore.PERMANENT_FAILURE_AND_SENT;
import static io.wisetime.connector.time_poster.long_polling.TimeGroupIdStore.SUCCESS_AND_SENT;

/**
 * @author pascal.filippi@gmail.com
 */
@Slf4j
public class TimeGroupStatusUpdater extends TimerTask implements HealthIndicator {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;
  private final TimeGroupIdStore timeGroupIdStore;
  private final ApiClient apiClient;
  private final AtomicBoolean runLock = new AtomicBoolean(false);
  private final AtomicReference<DateTime> lastSuccessfulRun;
  private final ExecutorService uploadExecutor;
  private final Timer timeGroupStatusUpdaterTimer;

  TimeGroupStatusUpdater(TimeGroupIdStore timeGroupIdStore, ApiClient apiClient) {
    this.timeGroupIdStore = timeGroupIdStore;
    this.apiClient = apiClient;
    this.lastSuccessfulRun = new AtomicReference<>(DateTime.now());
    // uploading statuses is mostly waiting on server response: We can afford a lot of parallelism
    this.uploadExecutor = Executors.newCachedThreadPool();
    this.timeGroupStatusUpdaterTimer = new Timer("status-uploader-timer", true);
  }

  @Override
  public void run() {
    if (runLock.compareAndSet(false, true)) {
      try {
        List<Pair<String, PostResult>> timeGroupStatuses = timeGroupIdStore.getAllWithPendingStatusUpdate();
        for (Pair<String, PostResult> timeGroupStatus: timeGroupStatuses) {
          updateTimeGroupStatus(timeGroupStatus.getLeft(), timeGroupStatus.getRight());
        }
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        log.error("Failed to update time group status", e);
      } finally {
        // ensure lock is released
        runLock.set(false);
      }
    } else {
      log.info("Skip status update, previous status update process is yet to complete");
    }
  }

  void startScheduler() {
    timeGroupStatusUpdaterTimer.scheduleAtFixedRate(this, TimeUnit.SECONDS.toMillis(30L),
        TimeUnit.SECONDS.toMillis(30L));
  }

  void stopScheduler() {
    timeGroupStatusUpdaterTimer.cancel();
    timeGroupStatusUpdaterTimer.purge();
  }

  void processSingle(String timeGroupId, PostResult result) {
    uploadExecutor.submit(() -> updateTimeGroupStatus(timeGroupId, result));
  }

  private void updateTimeGroupStatus(String timeGroupId, PostResult result) {
    try {
      // TRANSIENT_FAILURE doesn't need to be handled.
      // simply sending no update is considered a transient failure after a certain timeout
      switch (result.getStatus()) {
        case SUCCESS:
          apiClient.updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.SUCCESS)
              .timeGroupId(timeGroupId));
          timeGroupIdStore.putTimeGroupId(timeGroupId, SUCCESS_AND_SENT, result.getMessage().orElse(""));
          break;
        case PERMANENT_FAILURE:
          apiClient.updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.FAILURE)
              .timeGroupId(timeGroupId)
              .message(result.getMessage().orElse("Unexpected error while posting time")));
          timeGroupIdStore.putTimeGroupId(timeGroupId, PERMANENT_FAILURE_AND_SENT, result.getMessage().orElse(""));
          break;
        default:
          // do nothing
          log.debug("TRANSIENT_FAILURE for time group");
      }
    } catch (Exception e) {
      log.error("Error while updating posted time status.", e);
    }
  }

  @Override
  public boolean isHealthy() {
    return DateTime.now().minusMinutes(MAX_MINS_SINCE_SUCCESS).isBefore(lastSuccessfulRun.get());
  }
}
