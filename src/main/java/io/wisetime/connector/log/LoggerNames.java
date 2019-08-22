package io.wisetime.connector.log;

/**
 * @author dchandler
 */
public enum LoggerNames {

  HEART_BEAT_LOGGER_NAME("wt.connect.health");

  private final String loggerName;

  LoggerNames(String loggerName) {
    this.loggerName = loggerName;
  }

  public String getName() {
    return loggerName;
  }
}
