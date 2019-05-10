/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.long_polling;

import org.codejargon.fluentjdbc.api.query.Query;
import org.codejargon.fluentjdbc.api.query.UpdateResult;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.datastore.SQLiteHelper;

import static io.wisetime.connector.datastore.CoreLocalDbTable.TABLE_TIME_GROUPS_RECEIVED;

/**
 * A store for time groups ids for deduplication. Blocks a provided time group id for 30 minutes or until deleted.
 *
 * @author pascal.filippi@gmail.com
 */
class TimeGroupIdStore {

  private SQLiteHelper sqLiteHelper;

  TimeGroupIdStore(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(TABLE_TIME_GROUPS_RECEIVED);
  }

  Optional<String> alreadySeen(String timeGroupId) {
    return sqLiteHelper.query()
        .select("SELECT post_result FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=? AND received_timestamp > ?")
        .params(timeGroupId, System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30))
        .firstResult(rs -> rs.getString(1));
  }

  void putTimeGroupId(String timeGroupId, String postResult) {
    final Query query = sqLiteHelper.query();
    query.transaction().inNoResult(() -> {
      long timeStamp = System.currentTimeMillis();
      UpdateResult result = query.update("UPDATE " + TABLE_TIME_GROUPS_RECEIVED.getName() +
          " SET received_timestamp=?, post_result=? WHERE time_group_id=?")
          .params(timeStamp, postResult, timeGroupId)
          .run();

      if (result.affectedRows() == 0) {
        // new key value
        query.update("INSERT INTO " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " (time_group_id, post_result, received_timestamp, created_ts) VALUES (?,?,?,?)")
            .params(timeGroupId, postResult, timeStamp, System.currentTimeMillis())
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
