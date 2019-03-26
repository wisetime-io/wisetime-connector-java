/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A wrapper class around the tag upload process, that enforces a singleton runner pattern in the event that the previous
 * upload process has not completed prior to the next scheduled check.
 *
 * @author thomas.haines@practiceinsight.io
 */
public class TagUpsertTask extends TimerTask {

  private static final Logger log = LoggerFactory.getLogger(TagUpsertTask.class);
  private final Runnable tagRunner;
  private final AtomicBoolean runLock = new AtomicBoolean(false);
  private final AtomicReference<DateTime> lastSuccessfulRun;

  public TagUpsertTask(Runnable tagRunner) {
    this.tagRunner = tagRunner;
    this.lastSuccessfulRun = new AtomicReference<>(DateTime.now());
  }

  @Override
  public void run() {
    if (runLock.compareAndSet(false, true)) {
      try {
        tagRunner.run();
        lastSuccessfulRun.set(DateTime.now());
      } catch (Exception e) {
        LoggerFactory.getLogger(tagRunner.getClass()).error(e.getMessage(), e);
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
