/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.LifeCycle;
import io.wisetime.connector.config.RuntimeConfig;

/**
 * @author thomas.haines
 */
class LogbackConfiguratorTest {

  @Test
  void setLogLevel() {
    // if not configured, no action/exception
    LogbackConfigurator.setLogLevel();
  }

  @Test
  void addLocalAdapter() {
    // in env without config or explicit disable, should fail quietly
    RuntimeConfig.setProperty(() -> "DISABLE_AWS_CRED_USAGE", "true");
    Optional<Appender<ILoggingEvent>> localAdapter = LogbackConfigurator.createLocalAdapter();
    localAdapter.ifPresent(LifeCycle::stop);
  }

}