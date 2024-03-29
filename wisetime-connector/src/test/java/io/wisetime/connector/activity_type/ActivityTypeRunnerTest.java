/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.WiseTimeConnector;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class ActivityTypeRunnerTest {

  private WiseTimeConnector connector;
  private ActivityTypeRunner activityTypeRunner;

  @BeforeEach
  void setup() {
    connector = mock(WiseTimeConnector.class);
    activityTypeRunner = new ActivityTypeRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    ZonedDateTime startRun = activityTypeRunner.lastSuccessfulRun;
    Thread.sleep(1);
    activityTypeRunner.run();

    assertThat(activityTypeRunner.lastSuccessfulRun.toInstant().toEpochMilli())
        .as("expect last success was updated")
        .isGreaterThan(startRun.toInstant().toEpochMilli());

    verify(connector, times(1)).performActivityTypeUpdate();
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    activityTypeRunner.lastSuccessfulRun = ZonedDateTime.now().minusYears(1);
    assertThat(activityTypeRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }

}
