/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import com.amazonaws.services.logs.model.InputLogEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * Appender used at wise-sites for critical level logging for alerts and monitoring services.
 */
@SuppressWarnings("WeakerAccess")
public class WiseAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private final ConfigPojo.ConfigPojoBuilder configBuilder = ConfigPojo.ConfigPojoBuilder.aConfigPojo();
  private final LayoutEngineJson layoutEngine = new LayoutEngineJson();
  private ScheduledExecutorService scheduledExecutor;
  private CloudWriter cloudWriter = null;

  private AtomicBoolean initialised = new AtomicBoolean(false);

  @Override
  public void start() {
    layoutEngine.start();
    super.start();
  }

  void flushLogs() {
    if (cloudWriter != null) {
      try {
        cloudWriter.processLogEntries();
      } catch (Exception e) {
        System.err.println("AWS lib internal error " + e);
        addWarn("Internal error", e);
      }
    }
  }


  @Override
  protected void append(ILoggingEvent eventObject) {
    if (!isStarted()) {
      // if super.start() has not been run, do not write to log
      return;
    }

    if (!initialised.get()) {
      // to avoid any logging prior to parent starting do single init for append
      runInit();
    }

    if (cloudWriter != null) {
      InputLogEvent msg = new InputLogEvent();
      msg.setTimestamp(eventObject.getTimeStamp());
      String jsonStr = layoutEngine.doLayout(eventObject);
      msg.setMessage(jsonStr);
      cloudWriter.addMessageToQueue(msg);
    }
  }

  private synchronized void runInit() {
    if (initialised.compareAndSet(false, true)) {
      // SCHEDULER
      ThreadFactory threadFactory = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName("wise-log-" + UUID.randomUUID().toString().substring(0, 7));
        thread.setDaemon(true);
        return thread;
      };
      scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
      final ConfigPojo configPojo = configBuilder.build();

      Optional<CloudWriter> cloudWriterOpt = getOrCreateLogWriter(configPojo);
      cloudWriterOpt.ifPresent(writer -> {
            this.cloudWriter = writer;
            scheduledExecutor.scheduleWithFixedDelay(
                this::flushLogs,
                configPojo.getFlushIntervalInSeconds(),
                configPojo.getFlushIntervalInSeconds(),
                TimeUnit.SECONDS);
            layoutEngine.addConfig(configPojo, cloudWriter.getConfigPropertyMap());
          }
      );
    }
  }

  private Optional<CloudWriter> getOrCreateLogWriter(ConfigPojo configPojo) {
    return cloudWriter == null ? CloudWriter.createWriter(configPojo) : Optional.of(cloudWriter);
  }

  @Override
  public void stop() {
    if (cloudWriter != null) {
      scheduledExecutor.shutdown();
      try {
        scheduledExecutor.awaitTermination(6 * configBuilder.build().getFlushIntervalInSeconds(), TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        addWarn("Exception waiting for termination of scheduler", e);
      }

      flushLogs();
      cloudWriter.stop();
    }
    super.stop();
  }

  @SuppressWarnings("unused") // called via reflection
  public void setFlushIntervalInSeconds(int flushIntervalInSeconds) {
    configBuilder.withFlushIntervalInSeconds(flushIntervalInSeconds);
  }

  @SuppressWarnings("unused") // called via reflection
  @Deprecated
  public void setLogDefaultGroup(String logGroup) {
    configBuilder.withDefaultLogGroup(logGroup);
  }

  @SuppressWarnings("unused") // called via reflection
  public void setLogGroup(String logGroup) {
    configBuilder.withDefaultLogGroup(logGroup);
  }

  @SuppressWarnings("unused") // called via reflection
  public void setModuleName(String moduleName) {
    configBuilder.withModuleName(moduleName);
  }

  @SuppressWarnings("unused") // called via reflection
  public void setEnabled(String enabledStr) {
    configBuilder.withEnabled(enabledStr);
  }

  @SuppressWarnings("unused") // called via reflection
  public void setPropertiesFilePath(String propertiesFilePath) {
    configBuilder.withPropertiesFilePath(propertiesFilePath);
  }

  @SuppressWarnings("unused") // called via reflection
  public ConfigPojo getConfigPojo() {
    return configBuilder.build();
  }

  void reEnableForTest() {
    initialised.set(false);
  }

  void setLogsWriter(CloudWriter logsWriter) {
    this.cloudWriter = logsWriter;
  }
}
