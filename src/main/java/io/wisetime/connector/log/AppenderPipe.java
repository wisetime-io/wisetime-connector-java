/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import com.amazonaws.services.logs.model.InputLogEvent;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * This class is not intended to be constructed via the joran xml logback constructor. It is constructed explicitly, and
 * added to the existing joran xml configuration in an idempotent manner.
 *
 * @author thomas.haines
 */
class AppenderPipe extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final LoggingBridge bridge;
  private final ScheduledExecutorService scheduledExecutor;
  private final int flushIntervalInSeconds;
  private final LayoutEngineJson layoutEngine;

  AppenderPipe(LoggingBridge bridge,
               LayoutEngineJson layoutEngine,
               int flushIntervalInSeconds) {
    this.bridge = bridge;
    this.layoutEngine = layoutEngine;
    this.flushIntervalInSeconds = flushIntervalInSeconds;
    // SCHEDULER
    ThreadFactory threadFactory = r -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("log-pipe-" + UUID.randomUUID().toString().substring(0, 7));
      thread.setDaemon(true);
      return thread;
    };

    this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
  }

  AppenderPipe(LoggingBridge bridge) {
    this(bridge,
        new LayoutEngineJson(),
        // use default interval of 5 seconds
        5);
  }

  @Override
  public void start() {
    layoutEngine.start();
    scheduledExecutor.scheduleWithFixedDelay(
        this::flushLogs,
        flushIntervalInSeconds,
        flushIntervalInSeconds,
        TimeUnit.SECONDS);

    super.start();
  }

  final void flushLogs() {
    if (bridge == null) {
      // as precaution do not fail on NPE flush
      return;
    }
    try {
      bridge.flushMessages();
    } catch (Exception e) {
      System.err.println("AWS lib internal error " + e);
      addWarn("Internal error", e);
    }
  }

  @Override
  public void stop() {
    scheduledExecutor.shutdown();
    try {
      scheduledExecutor.awaitTermination(6 * flushIntervalInSeconds, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      addWarn("Exception waiting for termination of scheduler", e);
    }
    layoutEngine.stop();
    super.stop();

    flushLogs();
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (!super.isStarted()) {
      // if super.start() has not been run, do not write to log
      return;
    }
    if (bridge == null) {
      // as precaution do not fail on NPE of bridge
      return;
    }

    LogQueueCW.LogEntryCW logEntry = createLogEvent(eventObject);
    bridge.writeMessage(logEntry);
  }

  private LogQueueCW.LogEntryCW createLogEvent(ILoggingEvent eventObject) {
    final InputLogEvent msg = new InputLogEvent();
    msg.setTimestamp(eventObject.getTimeStamp());
    String jsonStr = layoutEngine.doLayout(eventObject);
    msg.setMessage(jsonStr);
    return new LogQueueCW.LogEntryCW(
        eventObject.getLevel(),
        eventObject.getLoggerName(),
        msg
    );
  }


}
