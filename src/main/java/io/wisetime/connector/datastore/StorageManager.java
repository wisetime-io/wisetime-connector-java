/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import java.util.Optional;

/**
 * @author thomas.haines@practiceinsight.io
 */
public interface StorageManager {

  Optional<String> getString(String key);

  void putString(String keyName, String value);

  Optional<Long> getLong(String key);

  void putLong(String keyName, long value);
}
