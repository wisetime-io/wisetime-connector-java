/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
@Getter
@ToString
@RequiredArgsConstructor
public class LocalDbTable {

  private final String name;
  private final String schema;
  private final List<Modification> modifications;

  @Getter
  @ToString
  @RequiredArgsConstructor
  static class Modification {
    private final String columnName;
    private final String sql;
  }
}
