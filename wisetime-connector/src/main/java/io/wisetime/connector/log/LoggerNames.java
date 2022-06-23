/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

/**
 * @author dchandler
 */
public enum LoggerNames {

  ROOT("root"),
  HEART_BEAT_LOGGER_NAME("wt.connect.health");

  private final String loggerName;

  LoggerNames(String loggerName) {
    this.loggerName = loggerName;
  }

  public String getName() {
    return loggerName;
  }
}
