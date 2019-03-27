/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public final class LocalDbTable {

  private String name;
  private String schema;

  public LocalDbTable(String name, String schema) {
    this.name = name;
    this.schema = schema;
  }

  public String getName() {
    return name;
  }

  public String getSchema() {
    return schema;
  }
}
