/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import com.google.common.collect.ImmutableList;

import java.util.Collections;

/**
 * Java connector SQL tables definition.
 *
 * @author galya.bogdanova@staff.wisetime.io
 */
public final class CoreLocalDbTable {

  /**
   * Table for key-value storage.
   * @see FileStore
   */
  public static final LocalDbTable TABLE_KEY_MAP = new LocalDbTable("key_map",
      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
          "key_name TEXT, " +
          "value TEXT ", Collections.emptyList());

  public static final LocalDbTable TABLE_TIME_GROUPS_RECEIVED = new LocalDbTable("time_groups_received",
      "time_group_id TEXT PRIMARY KEY, " +
          "created_ts INTEGER NOT NULL, " +
          "received_timestamp INTEGER NOT NULL, " +
          "post_result TEXT NOT NULL",
      ImmutableList.of(
          new LocalDbTable.Modification("message",
              "ALTER TABLE time_groups_received ADD COLUMN message TEXT NOT NULL DEFAULT ''")
      ));
}
