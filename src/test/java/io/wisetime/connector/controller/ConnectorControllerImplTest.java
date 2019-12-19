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
  void getTagSyncIntervalMinutes() {
    final ConnectorControllerImpl connectorController = (ConnectorControllerImpl) ConnectorController.newBuilder()
        .withApiKey("api key")
        .withWiseTimeConnector(mock(WiseTimeConnector.class))
        .disablePostedTimeFetching()
        .build();

    assertThat(connectorController.tagTaskSchedule.getPeriodMs())
        .as("Tag sync interval minutes shouldn't be changed without due consideration as it affects all " +
            "downstream connectors")
        .isEqualTo(TimeUnit.MINUTES.toMillis(5));
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
      controller.healthTaskSchedule.setInitialDelayMs(0);
      controller.healthTaskSchedule.setPeriodMs(3000);
      controller.start();

      verify(controller, times(1)).start();
      verify(controller, times(1)).stop();
      verify(wiseTimeConnector, times(1)).shutdown();
    });
  }
}