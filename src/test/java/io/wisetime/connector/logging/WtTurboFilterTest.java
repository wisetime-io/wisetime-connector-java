/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import ch.qos.logback.classic.Level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
class WtTurboFilterTest {

  private static SQLiteMessagePublisher sqLiteMessagePublisherMock;
  private static WtTurboFilter wtTurboFilter;

  @BeforeAll
  static void setup() {
    sqLiteMessagePublisherMock = mock(SQLiteMessagePublisher.class);
    wtTurboFilter = new WtTurboFilter(sqLiteMessagePublisherMock);
  }

  @BeforeEach
  void clean() {
    reset(sqLiteMessagePublisherMock);
  }

  @Test
  void testTurboFilter_publishCorrectLevel() {
    wtTurboFilter.decide(null, null, Level.TRACE, "Msg", null, null);
    wtTurboFilter.decide(null, null, Level.DEBUG, "Msg", null, null);
    wtTurboFilter.decide(null, null, Level.ERROR, "Msg", null, null);
    wtTurboFilter.decide(null, null, Level.INFO, "Msg", null, null);
    wtTurboFilter.decide(null, null, Level.WARN, "Msg", null, null);

    ArgumentCaptor<WtLog> wtLogArgumentCaptor = ArgumentCaptor.forClass(WtLog.class);
    verify(sqLiteMessagePublisherMock, times(5))
        .publish(wtLogArgumentCaptor.capture());

    List<WtLog> allLogs = wtLogArgumentCaptor.getAllValues();
    assertThat(allLogs.get(0).getLevel())
        .isEqualTo(WtLog.Level.INFO);
    assertThat(allLogs.get(1).getLevel())
        .isEqualTo(WtLog.Level.INFO);
    assertThat(allLogs.get(2).getLevel())
        .isEqualTo(WtLog.Level.ERROR);
    assertThat(allLogs.get(3).getLevel())
        .isEqualTo(WtLog.Level.INFO);
    assertThat(allLogs.get(4).getLevel())
        .isEqualTo(WtLog.Level.WARN);
  }

  @Test
  void testTurboFilter_noMessageNoThrowable_noMsgPublish() {
    wtTurboFilter.decide(null, null, Level.WARN, null, null, null);
    verify(sqLiteMessagePublisherMock, never())
        .publish(any(WtLog.class));
  }

  @Test
  void testTurboFilter_noMessageButThrowable_publishCorrectLogText() {
    final NullPointerException exception = new NullPointerException("NPE");
    wtTurboFilter.decide(null, null, null, null, null, exception);

    ArgumentCaptor<WtLog> wtLogArgumentCaptor = ArgumentCaptor.forClass(WtLog.class);
    verify(sqLiteMessagePublisherMock)
        .publish(wtLogArgumentCaptor.capture());

    assertThat(wtLogArgumentCaptor.getValue().getText())
        .contains(exception.getMessage());
  }

  @Test
  void testTurboFilter_getFormattedLog() {
    final String formattedLog = wtTurboFilter.getFormattedLog("This is {} not formatted text expecting {} message " +
        "formatting.", new Object[]{"one", "slf4j"});
    assertThat(formattedLog)
        .isEqualTo("This is one not formatted text expecting slf4j message formatting.");
  }

  @Test
  void testTurboFilter_publishCorrectLog() {
    final NullPointerException exception = new NullPointerException("NPE");
    wtTurboFilter.decide(null, null, null, "Text {} to format {}.",
        new Object[]{"test", "now"}, exception);

    ArgumentCaptor<WtLog> wtLogArgumentCaptor = ArgumentCaptor.forClass(WtLog.class);
    verify(sqLiteMessagePublisherMock)
        .publish(wtLogArgumentCaptor.capture());

    assertThat(wtLogArgumentCaptor.getValue().getText())
        .contains(exception.getMessage())
        .contains("Text test to format now.");

    assertThat(wtLogArgumentCaptor.getValue().getThread())
        .isEqualTo(Thread.currentThread().getName());
  }

}
