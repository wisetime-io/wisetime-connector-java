/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config.info;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Simple implementation of {@link ConnectorInfoProvider} which initializes connector info only once during construction and
 * always returns it on every {@link #get()} method invocation.
 *
 * @author yehor.lashkul
 */
public class ConstantConnectorInfoProvider implements ConnectorInfoProvider {

  private final ConnectorInfo connectorInfo;

  public ConstantConnectorInfoProvider() {
    ZoneOffset offset = OffsetDateTime.now().getOffset();
    connectorInfo = new ConnectorInfo(offset.toString());
  }

  @Override
  public ConnectorInfo get() {
    return connectorInfo;
  }
}
