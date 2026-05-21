/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.PERMANENT_FAILURE_AND_SENT;
import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.SUCCESS_AND_SENT;
import static io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore.TRANSIENT_FAILURE_AND_SENT;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import io.wisetime.connector.utils.BaseRunner;
import io.wisetime.generated.connect.TimeGroupStatus;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

/**
 * @author pascal.filippi@gmail.com
 */
@Slf4j
class TimeGroupStatusUpdater extends BaseRunner {

  private static final int MAX_MINS_SINCE_SUCCESS = 10;
  private final TimeGroupIdStore timeGroupIdStore;
  private final ApiClient apiClient;
  private final Timer timeGroupStatusUpdaterTimer;
  private final Supplier<ExecutorService> executorService;

  TimeGroupStatusUpdater(TimeGroupIdStore timeGroupIdStore, ApiClient apiClient, Supplier<ExecutorService> executorService) {
    this.timeGroupIdStore = timeGroupIdStore;
    this.apiClient = apiClient;
    this.executorService = executorService;
    // uploading statuses is mostly waiting on server response: We can afford a lot of parallelism
    this.timeGroupStatusUpdaterTimer = new Timer("status-uploader-timer", true);
  }

  @Override
  protected void performAction() {
    List<Pair<String, PostResult>> timeGroupStatuses = timeGroupIdStore.getAllWithPendingStatusUpdate();
    for (Pair<String, PostResult> timeGroupStatus: timeGroupStatuses) {
      updateTimeGroupStatus(timeGroupStatus.getLeft(), timeGroupStatus.getRight());
    }
  }

  @Override
  protected Logger getLogger() {
    return log;
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
    executorService.get().submit(() -> updateTimeGroupStatus(timeGroupId, result));
  }

  private void updateTimeGroupStatus(String timeGroupId, PostResult result) {
    try {
      log.info("Processed time group {}, result: {}", timeGroupId, result);
      switch (result.getStatus()) {
        case SUCCESS:
          apiClient.updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.SUCCESS)
              .externalId(result.getExternalId().orElse(""))
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
        case TRANSIENT_FAILURE:
          apiClient.updatePostedTimeStatus(new TimeGroupStatus()
              .status(TimeGroupStatus.StatusEnum.RETRIABLE_FAILURE)
              .timeGroupId(timeGroupId)
              .message(result.getMessage().orElse("Unexpected error while posting time")));
          timeGroupIdStore.putTimeGroupId(timeGroupId, TRANSIENT_FAILURE_AND_SENT, result.getMessage().orElse(""));
          break;
        default:
          log.warn("Unknown post result status to update time group status: {}", result.getStatus());
      }
    } catch (Exception e) {
      log.error("Error while updating posted time status.", e);
    }
  }

  @Override
  protected int getMaxMinsSinceSuccess() {
    return MAX_MINS_SINCE_SUCCESS;
  }
}
