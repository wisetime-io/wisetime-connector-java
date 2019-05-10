/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import java.util.Optional;

import io.wisetime.connector.WiseTimeConnector;

/**
 * {@link WiseTimeConnector} implementations can use this storage to persist data between
 * iterations.
 *
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public interface ConnectorStore {

  /**
   * Retrieve a String value from the store
   *
   * @param key to use to retrieve the value from the store
   * @return the String value that was previously stored, or an empty Optional if there is no value stored for the key
   */
  Optional<String> getString(String key);

  /**
   * Persist a string value to the store
   *
   * @param key an identifier that can subsequently be used to retrieve the value from the store
   * @param value the value to persist
   */
  void putString(String key, String value);

  /**
   * Retrieve a Long value from the store
   *
   * @param key to use to retrieve the value from the store
   * @return the Long value that was previously stored, or an empty Optional if there is no value stored for the key
   */
  Optional<Long> getLong(String key);

  /**
   * Persist a long value to the store
   *
   * @param key an identifier that can subsequently be used to retrieve the value from the store
   * @param value the value to persist
   */
  void putLong(String key, long value);
}
