/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static io.wisetime.connector.log.LoggerNames.HEART_BEAT_LOGGER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.LifeCycle;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

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

  @SuppressWarnings("unchecked")
  @Test
  void addHeartBeatAdapter() {
    Optional<Appender<ILoggingEvent>> heartBeatAdapter = LogbackConfigurator.createLocalAdapter();
    heartBeatAdapter.ifPresent(LifeCycle::start);
    LogbackConfigurator.configBaseHeartBeatLogging(heartBeatAdapter.get());

    final Appender<ILoggingEvent> appender = mock(Appender.class);
    final ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);

    final Logger heartBeatLogger = (Logger) LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName());

    heartBeatAdapter.ifPresent(heartBeatLogger::addAppender);
    heartBeatLogger.addAppender(appender);

    LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName()).info("WISE_CONNECT_HEARTBEAT success");
    assertThat(heartBeatLogger.getAppender(heartBeatAdapter.get().getName())).isNotNull();
    verify(appender, atLeastOnce()).doAppend(captor.capture());

    final List<ILoggingEvent> events = captor.getAllValues();
    assertThat(events).areAtLeastOne(new Condition<ILoggingEvent>() {

      @Override
      public boolean matches(ILoggingEvent loggingEvent) {
        return Objects.equals(loggingEvent.getMessage(), "WISE_CONNECT_HEARTBEAT success");
      }
    });
  }

  @Test
  void refreshLocalAdapterCWCredentials() {
    final ManagedConfigResponse config = mock(ManagedConfigResponse.class);
    final LocalAdapterCW adapterCWMock = mock(LocalAdapterCW.class);

    final AppenderPipe appenderPipe = new AppenderPipe(adapterCWMock);
    appenderPipe.setName(LocalAdapterCW.class.getSimpleName());

    final Logger heartBeatLogger = (Logger) LoggerFactory.getLogger("test-logger");
    heartBeatLogger.addAppender(appenderPipe);

    LogbackConfigurator.refreshLocalAdapterCWCredentials(heartBeatLogger, config);
    verify(adapterCWMock, times(1)).init(config);
  }
}