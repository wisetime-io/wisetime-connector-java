/*
 * Copyright (c) 2026 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import java.util.Map;
import java.util.Objects;

/**
 * Named values to substitute into an {@link io.wisetime.connector.api_client.EndpointPath}'s {@code {name}}
 * path placeholders, e.g. the {@code fieldId} in {@code timesheetfield/{fieldId}/option/batch}.
 */
public final class PathParams {

  private static final PathParams NONE = new PathParams(Map.of());

  private final Map<String, String> values;

  private PathParams(Map<String, String> values) {
    this.values = values;
  }

  public static PathParams none() {
    return NONE;
  }

  public static PathParams of(String key, String value) {
    return new PathParams(Map.of(key, value));
  }

  Map<String, String> getValues() {
    return values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PathParams)) {
      return false;
    }
    return values.equals(((PathParams) o).values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values);
  }
}
