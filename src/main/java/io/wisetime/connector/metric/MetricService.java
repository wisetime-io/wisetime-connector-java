/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in memory metric service.
 * <p/>
 * You can increment metric statistics using {@link #increment(Metric)} method and get all gathered metric information using
 * {@link #getMetrics()}
 * <p/>
 * This class is fully threadsafe
 *
 * @author yehor.lashkul
 */
public class MetricService {

  private final Map<Metric, Integer> metrics = new ConcurrentHashMap<>();

  /**
   * Increments specified metric
   *
   * @param metric to increment
   */
  public void increment(Metric metric) {
    increment(metric, 1);
  }

  public void increment(Metric metric, int incrementBy) {
    metrics.compute(metric, (key, count) -> count != null ? count + incrementBy : incrementBy);
  }

  /**
   * Returns metrics gathered from the start of the application.
   *
   * @return {@link MetricInfo} object as a representation of the gathered metrics
   */
  public MetricInfo getMetrics() {
    int processedTags = metrics.getOrDefault(Metric.TAG_PROCESSED, 0);
    int processedTimeGroups = metrics.getOrDefault(Metric.TIME_GROUP_PROCESSED, 0);
    return new MetricInfo(processedTags, processedTimeGroups);
  }
}
