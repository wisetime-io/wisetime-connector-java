/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

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
          "value TEXT ");
}
