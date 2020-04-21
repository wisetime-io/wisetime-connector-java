/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.deduplication;

import lombok.Data;
import lombok.NonNull;

/**
 * Marker wrapper class for String to make developer's life easier.
 *
 * @author thomas.haines
 */
@Data
public class TimeGroupId {

  @NonNull
  private final String value;

  @Override
  public String toString() {
    return value;
  }

  public static TimeGroupId create(String value) {
    return new TimeGroupId(value);
  }

}
