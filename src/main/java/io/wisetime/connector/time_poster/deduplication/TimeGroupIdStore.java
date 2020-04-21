/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.deduplication;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.codejargon.fluentjdbc.api.query.Query;
import org.codejargon.fluentjdbc.api.query.UpdateResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.datastore.SQLiteHelper;

import static io.wisetime.connector.datastore.CoreLocalDbTable.TABLE_TIME_GROUPS_RECEIVED;

/**
 * A store for time groups ids for deduplication. Blocks a provided time group id IN_PROGRESS for 5 minutes.
 * Other status will be returned as stored.
 *
 * @author pascal.filippi@gmail.com
 */
public class TimeGroupIdStore {

  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String SUCCESS_AND_SENT = "SUCCESS_AND_SENT";
  public static final String PERMANENT_FAILURE_AND_SENT = "PERMANENT_FAILURE_AND_SENT";
  public static final String TRANSIENT_FAILURE_AND_SENT = "TRANSIENT_FAILURE_AND_SENT";
  // Time in minutes
  private static final long MAX_IN_PROGRESS_TIME = 8;
  // Time in days
  private static final long MAX_STATUS_STORAGE_TIME = 60;

  private final SQLiteHelper sqLiteHelper;

  public TimeGroupIdStore(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(TABLE_TIME_GROUPS_RECEIVED);
  }

  private void deleteOldRecords() {
    sqLiteHelper.query().update("DELETE FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
        " WHERE received_timestamp < ?")
        .params(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_STATUS_STORAGE_TIME))
        .run();
  }

  /**
   *
   * @return A Map of timeGroupId
   */
  public Map<TimeGroupId, String> fetchPostResultList(List<TimeGroupId> timeGroupIdList) {
    Map<TimeGroupId, String> resultMap = new HashMap<>();
    sqLiteHelper.query()
        // always return status for SUCCESS, TRANSIENT_FAILURE and PERMANENT_FAILURE
        // If a time group is IN_PROGRESS for more than 5 minutes: assume failure and allow to try again
        .select("SELECT time_group_id, post_result FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id IN :timeGroupIdList")
        .params(timeGroupIdList)
        .iterateResult(rs -> {
          resultMap.put(
              TimeGroupId.create(rs.getString(1)),
              rs.getString(2));
        });
    return resultMap;
  }

  public Optional<String> alreadySeenFetchClient(String timeGroupId) {
    return sqLiteHelper.query()
        // always return status for SUCCESS, TRANSIENT_FAILURE and PERMANENT_FAILURE
        // If a time group is IN_PROGRESS for more than 8 minutes: assume failure and allow to try again
        .select("SELECT post_result FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=? AND (received_timestamp > ? or post_result != ?)")
        .params(timeGroupId,
            System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MAX_IN_PROGRESS_TIME),
            IN_PROGRESS)
        .firstResult(rs -> rs.getString(1));
  }

  /**
   * This method returns the status of the time group irrespective of its age.
   * For checking if we need to process a time group use alreadySeenFetchClient.
   * This one is used as a safeguard to prevent reprocessing caused by a processing time greater than the retry timeout
   */
  public Optional<String> getPostStatusForFetchClient(String timeGroupId) {
    return sqLiteHelper.query()
        .select("SELECT post_result FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=?")
        .params(timeGroupId)
        .firstResult(rs -> rs.getString(1));
  }

  public Optional<PostResult> alreadySeenWebHook(String timeGroupId) {
    return sqLiteHelper.query()
        // always return status for SUCCESS, TRANSIENT_FAILURE and PERMANENT_FAILURE
        // If a time group is IN_PROGRESS for more than 5 minutes: assume failure and allow to try again
        .select("SELECT post_result, message FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=? AND (received_timestamp > ? or post_result != ?)")
        .params(timeGroupId,
            System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MAX_IN_PROGRESS_TIME),
            IN_PROGRESS)
        .firstResult(rs -> PostResult.valueOf(rs.getString(1)).withMessage(rs.getString(2)));
  }

  public List<Pair<String, PostResult>> getAllWithPendingStatusUpdate() {
    return sqLiteHelper.query()
        // Get all statuses with SUCCESS or PERMANENT_FAILURE for updating
        // give the async immediate status updater some time to complete before retrying by table sweep
        .select("SELECT time_group_id, post_result, message FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE (post_result = :success or post_result = :permFail) and received_timestamp < :ts")
        .namedParam("success", PostResultStatus.SUCCESS.name())
        .namedParam("permFail", PostResultStatus.PERMANENT_FAILURE.name())
        .namedParam("ts", System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1))
        .listResult(rs -> Pair.of(rs.getString(1),
            PostResult.valueOf(rs.getString(2)).withMessage(rs.getString(3))));
  }

  public void putTimeGroupId(String timeGroupId, String postResult, String message) {
    // purge old records when inserting new ones
    deleteOldRecords();
    final Query query = sqLiteHelper.query();
    query.transaction().inNoResult(() -> {
      long timeStamp = System.currentTimeMillis();
      UpdateResult result = query.update("UPDATE " + TABLE_TIME_GROUPS_RECEIVED.getName() +
          " SET received_timestamp=?, post_result=?, message=? WHERE time_group_id=?")
          .params(timeStamp, postResult, message, timeGroupId)
          .run();

      if (result.affectedRows() == 0) {
        // new key value
        query.update("INSERT INTO " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " (time_group_id, post_result, received_timestamp, created_ts, message) VALUES (?,?,?,?,?)")
            .params(timeGroupId, postResult, timeStamp, timeStamp, message)
            .run();
      }
    });
  }

  public void deleteTimeGroupId(String timeGroupId) {
    sqLiteHelper.query().update("DELETE FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() + " WHERE time_group_id=?")
        .params(timeGroupId)
        .run();
  }
}
