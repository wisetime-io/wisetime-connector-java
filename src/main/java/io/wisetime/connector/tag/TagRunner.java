/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.wisetime.connector.WiseTimeConnector;
import lombok.extern.slf4j.Slf4j;

/**
 * A wrapper class around the tag upload process, that enforces a singleton runner pattern in the event that the previous
 * upload process has not completed prior to the next scheduled check.
 *
 * @author thomas.haines@practiceinsight.io
 */
@Slf4j
public class TagRunner extends TimerTask {

  private final WiseTimeConnector connector;
  private final AtomicBoolean runLock = new AtomicBoolean(false);
  private final AtomicReference<DateTime> lastSuccessfulRun;

  public TagRunner(WiseTimeConnector connector) {
    this.connector = connector;
    this.lastSuccessfulRun = new AtomicReference<>(DateTime.now());
  }

  @Override
  public void run() {
    if (runLock.compareAndSet(false, true)) {
      try {
        connector.performTagUpdate();
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        LoggerFactory.getLogger(connector.getClass()).error(e.getMessage(), e);
      } finally {
        // ensure lock is released
        runLock.set(false);
      }
    } else {
      log.info("Skip tag runner timer instantiation, previous tag upload process is yet to complete");
    }
  }

  public DateTime getLastSuccessfulRun() {
    return lastSuccessfulRun.get();
  }
}
