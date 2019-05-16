/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

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
 * A store for time groups ids for deduplication. Blocks a provided time group id for 30 minutes or until deleted.
 *
 * @author pascal.filippi@gmail.com
 */
class TimeGroupIdStore {

  static final String IN_PROGRESS = "IN_PROGRESS";
  static final String SUCCESS_AND_SENT = "SUCCESS_AND_SENT";
  static final String PERMANENT_FAILURE_AND_SENT = "PERMANENT_FAILURE_AND_SENT";
  private static final long MAX_IN_PROGRESS_TIME = 5;

  private SQLiteHelper sqLiteHelper;

  TimeGroupIdStore(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(TABLE_TIME_GROUPS_RECEIVED);
  }

  Optional<String> alreadySeen(String timeGroupId) {
    return sqLiteHelper.query()
        // always return status for SUCCESS, TRANSIENT_FAILURE and PERMANENT_FAILURE
        // If a time group is IN_PROGRESS for more than 5 minutes: assume failure and allow to try again
        .select("SELECT post_result FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=? AND (received_timestamp > ? or post_result != ?)")
        .params(timeGroupId,
            System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(MAX_IN_PROGRESS_TIME),
            IN_PROGRESS)
        .firstResult(rs -> rs.getString(1));
  }

  List<Pair<String, PostResult>> getAllWithPendingStatusUpdate() {
    return sqLiteHelper.query()
        // Get all statuses with SUCCESS or PERMANENT_FAILURE for updating
        .select("SELECT time_group_id, post_result, message FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE post_result = ? or post_result = ?")
        .params(PostResultStatus.SUCCESS.name(), PostResultStatus.PERMANENT_FAILURE.name())
        .listResult(rs -> Pair.of(rs.getString(1),
            PostResult.valueOf(rs.getString(2)).withMessage(rs.getString(3))));
  }

  void putTimeGroupId(String timeGroupId, String postResult, String message) {
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
            .params(timeGroupId, postResult, timeStamp, System.currentTimeMillis(), message)
            .run();
      }
    });
  }

  void deleteTimeGroupId(String timeGroupId) {
    sqLiteHelper.query().update("DELETE FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() + " WHERE time_group_id=?")
        .params(timeGroupId)
        .run();
  }
}
