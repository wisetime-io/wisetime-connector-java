/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import java.util.Optional;

/**
 * @author thomas.haines@practiceinsight.io
 */
public interface StorageManager {

  Optional<String> findValue(String keyName);

  void setValue(String keyName, String value);
}
