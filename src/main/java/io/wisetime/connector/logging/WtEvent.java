/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class WtEvent {

  public enum Type {
    SERVER_STARTED,
    HEALTH_CHECK_FAILED,
    HEALTH_CHECK_SUCCESS,
    HEALTH_CHECK_MAX_SUCCESSIVE_FAILURES,
    TAGS_UPSERTED,
    TIME_GROUP_POSTED;

    public static Optional<Type> fromName(String name) {
      return Arrays.stream(Type.values())
          .filter(type -> type.name().equals(name))
          .findAny();
    }

    @Override
    public String toString() {
      return "Type{" +
          "name='" + name() + '\'' +
          '}';
    }
  }

  private int id;
  private long timestamp;
  private Type type;
  private String data;

  public WtEvent(Type type) {
    this.timestamp = new Date().getTime();
    this.type = type;
  }

  public WtEvent(Type type, String data) {
    this(type);
    this.data = data;
  }

  public WtEvent(int id, long timestamp, Type type, String data) {
    this.id = id;
    this.timestamp = timestamp;
    this.type = type;
    this.data = data;
  }

  public int getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Type getType() {
    return type;
  }

  public String getData() {
    return data;
  }

}
