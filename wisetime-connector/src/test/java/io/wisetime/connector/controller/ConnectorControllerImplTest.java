/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.ConnectorController;
import io.wisetime.connector.WiseTimeConnector;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class ConnectorControllerImplTest {

  @Test
  void checkSyncIntervals() {
    final ConnectorControllerImpl connectorController = (ConnectorControllerImpl) ConnectorController.newBuilder()
        .withApiKey("api key")
        .withWiseTimeConnector(mock(WiseTimeConnector.class))
        .disablePostedTimeFetching()
        .build();

    assertThat(connectorController.getTagTaskScheduleMins())
        .as("Tag sync interval minutes shouldn't be changed without due consideration as it affects all "
        + "downstream connectors")
        .isEqualTo(1);

    assertThat(connectorController.getTagSlowLoopTaskSchedule().getPeriodMs())
        .as("Tag slow loop sync interval minutes shouldn't be changed without due consideration as it affects all "
        + "downstream connectors")
        .isEqualTo(TimeUnit.MINUTES.toMillis(5));

    assertThat(connectorController.getActivityTypeTaskScheduleMins())
        .as("Activity type sync interval minutes shouldn't be changed without due consideration as it affects all "
        + "downstream connectors")
        .isEqualTo(5);

    assertThat(connectorController.getActivityTypeSlowLoopTaskSchedule().getPeriodMs())
        .as("Activity type slow loop sync interval minutes shouldn't be changed without due consideration as it affects all "
            + "downstream connectors")
        .isEqualTo(TimeUnit.MINUTES.toMillis(15));
  }

  @Test
  void stop() {
    Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
      final WiseTimeConnector wiseTimeConnector = mock(WiseTimeConnector.class);
      ConnectorControllerImpl controller = spy((ConnectorControllerImpl) ConnectorController.newBuilder()
          .withApiKey("api key")
          .withWiseTimeConnector(wiseTimeConnector)
          .useTagsOnly()
          .build());
      controller.getHealthTaskSchedule().setInitialDelayMs(0);
      controller.getHealthTaskSchedule().setPeriodMs(3000);
      controller.start();

      verify(controller, times(1)).start();
      verify(controller, times(1)).stop();
      verify(wiseTimeConnector, times(1)).shutdown();
    });
  }
}
