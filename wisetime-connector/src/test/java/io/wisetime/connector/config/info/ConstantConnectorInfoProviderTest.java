/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config.info;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class ConstantConnectorInfoProviderTest {

  @Test
  void testProvidesConstantValue() {
    ConnectorInfoProvider connectorInfoProvider = new ConstantConnectorInfoProvider();

    ConnectorInfo connectorInfo = connectorInfoProvider.get();

    assertThat(connectorInfoProvider.get())
        .as("Always returns the same value")
        .isSameAs(connectorInfo);
  }
}
