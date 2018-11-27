/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

/**
 * @author thomas.haines@practiceinsight.io
 */
public interface RuntimeConfigKey {

  /**
   * @return Value used for the environment variable or system property (case insensitive).
   */
  String getConfigKey();
}
