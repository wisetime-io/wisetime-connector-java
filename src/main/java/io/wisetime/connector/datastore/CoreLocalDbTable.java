/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public final class CoreLocalDbTable {
  public static final LocalDbTable TABLE_KEY_MAP = new LocalDbTable("key_map",
      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
          "key_name TEXT, " +
          "value TEXT ");
  public static final LocalDbTable TABLE_LOG = new LocalDbTable("log",
      "log_id INTEGER PRIMARY KEY, " +
          "log_timestamp INTEGER NOT NULL, " +
          "log_level INTEGER NOT NULL, " +
          "log_thread VARCHAR, " +
          "log_text TEXT NOT NULL");
  public static final LocalDbTable TABLE_EVENT = new LocalDbTable("event",
      "event_id INTEGER PRIMARY KEY, " +
          "event_timestamp INTEGER NOT NULL, " +
          "event_name TEXT NOT NULL, " +
          "event_data TEXT");

}
