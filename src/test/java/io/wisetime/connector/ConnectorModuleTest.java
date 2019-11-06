/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author shane.xie
 */
class ConnectorModuleTest {

  @Test
  void getTagSyncIntervalMinutes() {
    assertThat(ConnectorModule.getTagSyncIntervalMinutes())
        .isEqualTo(5)
        .as("Tag sync interval minutes shouldn't be changed without due consideration as it affects all " +
            "downstream connectors");
  }
}