/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author thomas.haines
 */
class AppenderPipeTest {
  private final Faker faker = new Faker();

  private LoggingBridge bridgeMock;
  private AppenderPipe appenderPipe;
  private LayoutEngineJson layoutMock;


  @BeforeEach
  void setup() {
    bridgeMock = mock(LoggingBridge.class);
    layoutMock = mock(LayoutEngineJson.class);
    appenderPipe = new AppenderPipe(bridgeMock, layoutMock, 1);
  }

  @AfterEach
  void tearDown() {
    appenderPipe.stop();
  }

  @Test
  void testWrite() {
    appenderPipe.start();
    ILoggingEvent eventMock = mock(ILoggingEvent.class);
    Level logLevel = Level.WARN;

    when(eventMock.getLevel()).thenReturn(logLevel);
    String logName = faker.bothify("logName#####");
    when(eventMock.getLoggerName()).thenReturn(logName);
    String theMsg = faker.bothify("textMsg#####");
    when(layoutMock.doLayout(eq(eventMock))).thenReturn(theMsg);

    appenderPipe.append(eventMock);

    ArgumentCaptor<LogQueueCW.LogEntryCW> logEntryCaptor = ArgumentCaptor.forClass(LogQueueCW.LogEntryCW.class);
    verify(bridgeMock, times(1)).writeMessage(logEntryCaptor.capture());

    LogQueueCW.LogEntryCW result = logEntryCaptor.getValue();

    assertThat(result.getInputLogEvent().message())
        .isEqualTo(theMsg);
    assertThat(result.getLevel())
        .isEqualTo(logLevel);
    assertThat(result.getLoggerName())
        .isEqualTo(logName);

  }

}
