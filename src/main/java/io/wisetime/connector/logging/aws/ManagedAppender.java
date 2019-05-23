/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging.aws;

import com.google.common.base.Preconditions;

import com.amazonaws.services.logs.model.InputLogEvent;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * Appender used at wise-sites for critical level logging for alerts and monitoring services.
 */
@SuppressWarnings({"WeakerAccess", "Duplicates"})
public class ManagedAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final LayoutEngineJson layoutEngine = new LayoutEngineJson();
  private final BufferedLogWriter bufferedLogWriter;


  public ManagedAppender(BufferedLogWriter bufferedLogWriter) {
    Preconditions.checkNotNull(bufferedLogWriter, "illegal null value for bufferedLogWriter");
    this.bufferedLogWriter = bufferedLogWriter;

  }

  @Override
  public void start() {
    layoutEngine.start();
    super.start();
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (!isStarted()) {
      // if super.start() has not been run, do not write to log
      return;
    }

    InputLogEvent msg = new InputLogEvent();
    msg.setTimestamp(eventObject.getTimeStamp());
    String jsonStr = layoutEngine.doLayout(eventObject);
    msg.setMessage(jsonStr);
    bufferedLogWriter.addMessageToQueue(msg);
  }


  @Override
  public void stop() {
    super.stop();
    try {
      bufferedLogWriter.stop();
    } catch (InterruptedException e) {
      addWarn("Exception waiting for termination of scheduler", e);
    }
  }

}
