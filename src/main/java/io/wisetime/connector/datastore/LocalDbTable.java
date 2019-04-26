/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

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

}
