/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import java.util.Arrays;
import java.util.Date;

import javax.validation.constraints.NotNull;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class WtLog implements Comparable<WtLog> {

  public enum Level {
    INFO(0),
    WARN(1),
    ERROR(2);

    public static Level fromNumeric(int number) {
      return Arrays.stream(Level.values())
          .filter(level -> level.number == number)
          .findAny()
          .orElse(null);
    }

    private int number;

    Level(int number) {
      this.number = number;
    }

    public int getNumeric() {
      return number;
    }
  }

  private int id;
  private long timestamp;
  private Level level;
  private String thread;
  private String text;

  public WtLog(Level level, String thread, String text) {
    this(0, new Date().getTime(), level, thread, text);
  }

  public WtLog(int id, long timestamp, Level level, String thread, String text) {
    this.id = id;
    this.timestamp = timestamp;
    this.level = level;
    this.thread = thread;
    this.text = text;
  }

  public int getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Level getLevel() {
    return level;
  }

  public String getThread() {
    return thread;
  }

  public String getText() {
    return text;
  }

  /**
   * Sorts descending by timestamp - first are the most recent.
   */
  @Override
  public int compareTo(@NotNull WtLog log) {
    if (timestamp == log.getTimestamp()) {
      return 0;
    }
    return timestamp > log.getTimestamp() ? -1 : 1;
  }
}
