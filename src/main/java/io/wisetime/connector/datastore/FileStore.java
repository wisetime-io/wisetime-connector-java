/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import org.codejargon.fluentjdbc.api.query.Query;
import org.codejargon.fluentjdbc.api.query.UpdateResult;

import java.util.Optional;

import static io.wisetime.connector.datastore.CoreLocalDbTable.TABLE_KEY_MAP;

/**
 * Sqlite implementation of {@link ConnectorStore}. If {@link io.wisetime.connector.config.ConnectorConfigKey#DATA_DIR} is
 * set - data will survive though application restarts, otherwise this is not guaranteed.
 *
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public class FileStore implements ConnectorStore {

  private SQLiteHelper sqLiteHelper;

  public FileStore(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(TABLE_KEY_MAP);
  }

  @Override
  public Optional<String> getString(String key) {
    return sqLiteHelper.query()
        .select("SELECT value FROM " + TABLE_KEY_MAP.getName() + " WHERE key_name=?")
        .params(key)
        .firstResult(rs -> rs.getString(1));
  }

  @Override
  public void putString(String key, String value) {
    final Query query = sqLiteHelper.query();
    query.transaction().inNoResult(() -> {
      UpdateResult result = query.update("UPDATE " + TABLE_KEY_MAP.getName() + " SET value=? WHERE key_name=?")
          .params(value, key)
          .run();
      if (result.affectedRows() == 0) {
        // new key value
        query.update("INSERT INTO " + TABLE_KEY_MAP.getName() + " (key_name,value) VALUES (?,?)")
            .params(key, value)
            .run();
      }
    });
  }

  @Override
  public Optional<Long> getLong(String key) {
    return getString(key).flatMap(value -> {
      try {
        return Optional.of(Long.valueOf(value));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    });
  }

  @Override
  public void putLong(String key, long value) {
    putString(key, String.valueOf(value));
  }
}
