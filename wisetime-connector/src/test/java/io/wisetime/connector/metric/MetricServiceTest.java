/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class MetricServiceTest {

  private ExecutorService executor = Executors.newFixedThreadPool(2);
  private MetricService metricService;

  @BeforeEach
  void setup() {
    metricService = new MetricService();
  }

  @Test
  void metrics_empty() {
    MetricInfo metrics = metricService.getMetrics();
    assertThat(metrics).isNotNull();
    assertThat(metrics.getProcessedTags()).isEqualTo(0);
    assertThat(metrics.getProcessedTimeGroups()).isEqualTo(0);
  }

  @Test
  void increment_sync() {
    IntStream.range(0, 3).forEach(unused -> metricService.increment(Metric.TAG_PROCESSED));
    IntStream.range(0, 2).forEach(unused -> metricService.increment(Metric.TIME_GROUP_PROCESSED));

    MetricInfo metrics = metricService.getMetrics();
    assertThat(metrics).isNotNull();
    assertThat(metrics.getProcessedTags()).isEqualTo(3);
    assertThat(metrics.getProcessedTimeGroups()).isEqualTo(2);
  }

  @Test
  void increment_concurrent() {
    Stream<Runnable> tagIncrements =
        IntStream.range(0, 3).mapToObj(unused -> () -> metricService.increment(Metric.TAG_PROCESSED));
    Stream<Runnable> timeGroupIncrements =
        IntStream.range(0, 2).mapToObj(unused -> () -> metricService.increment(Metric.TIME_GROUP_PROCESSED));
    Stream.concat(tagIncrements, timeGroupIncrements).map(executor::submit).forEach(this::futureGet);

    MetricInfo metrics = metricService.getMetrics();
    assertThat(metrics).isNotNull();
    assertThat(metrics.getProcessedTags()).isEqualTo(3);
    assertThat(metrics.getProcessedTimeGroups()).isEqualTo(2);
  }

  @Test
  void incrementBy_sync() {
    IntStream.range(0, 3).forEach(unused -> metricService.increment(Metric.TAG_PROCESSED, 2));
    IntStream.range(0, 2).forEach(unused -> metricService.increment(Metric.TIME_GROUP_PROCESSED, 2));

    MetricInfo metrics = metricService.getMetrics();
    assertThat(metrics).isNotNull();
    assertThat(metrics.getProcessedTags()).isEqualTo(6); // 3 times * increment by 2
    assertThat(metrics.getProcessedTimeGroups()).isEqualTo(4); // 2 times * increment by 2
  }

  @Test
  void incrementBy_concurrent() {
    Stream<Runnable> tagIncrements =
        IntStream.range(0, 3).mapToObj(unused -> () -> metricService.increment(Metric.TAG_PROCESSED, 2));
    Stream<Runnable> timeGroupIncrements =
        IntStream.range(0, 2).mapToObj(unused -> () -> metricService.increment(Metric.TIME_GROUP_PROCESSED, 2));
    Stream.concat(tagIncrements, timeGroupIncrements).map(executor::submit).forEach(this::futureGet);

    MetricInfo metrics = metricService.getMetrics();
    assertThat(metrics).isNotNull();
    assertThat(metrics.getProcessedTags()).isEqualTo(6); // 3 times * increment by 2
    assertThat(metrics.getProcessedTimeGroups()).isEqualTo(4); // 2 times * increment by 2
  }

  @SuppressWarnings("UnusedReturnValue")
  private <T> T futureGet(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
