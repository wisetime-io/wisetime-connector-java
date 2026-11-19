/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config.info;

/**
 * @author yehor.lashkul
 */
@FunctionalInterface
public interface ConnectorInfoProvider {
  ConnectorInfo get();
}
