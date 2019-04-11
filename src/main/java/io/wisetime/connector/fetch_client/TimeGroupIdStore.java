/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.fetch_client;

import org.codejargon.fluentjdbc.api.query.Query;
import org.codejargon.fluentjdbc.api.query.UpdateResult;

import java.util.concurrent.TimeUnit;

import io.wisetime.connector.datastore.SQLiteHelper;

import static io.wisetime.connector.datastore.CoreLocalDbTable.TABLE_TIME_GROUPS_RECEIVED;

/**
 * A store for time groups ids for deduplication. Blocks a provided time group id for 30 minutes or until deleted.
 *
 * @author pascal.filippi@gmail.com
 */
public class TimeGroupIdStore {

  private SQLiteHelper sqLiteHelper;

  public TimeGroupIdStore(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(TABLE_TIME_GROUPS_RECEIVED);
  }

  public boolean alreadySeen(String timeGroupId) {
    return sqLiteHelper.query()
        .select("SELECT received_timestamp FROM " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " WHERE time_group_id=? AND received_timestamp > ?")
        .params(timeGroupId, System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30))
        .firstResult(rs -> rs.getLong(1))
        .isPresent();
  }

  public void putTimeGroupId(String timeGroupId) {
    final Query query = sqLiteHelper.query();
    query.transaction().inNoResult(() -> {
      long timeStamp = System.currentTimeMillis();
      UpdateResult result = query.update("UPDATE " + TABLE_TIME_GROUPS_RECEIVED.getName() +
          " SET received_timestamp=? WHERE time_group_id=?")
          .params(timeStamp, timeGroupId)
          .run();
      if (result.affectedRows() == 0) {
        // new key value
        query.update("INSERT INTO " + TABLE_TIME_GROUPS_RECEIVED.getName() +
            " (time_group_id,received_timestamp) VALUES (?,?)")
            .params(timeGroupId, timeStamp)
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
