/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.health.HealthIndicator;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class around the tag slow sync process, that enforces a singleton runner pattern in the event
 * that the previous upload process has not completed prior to the next scheduled check.
 *
 * @author pascal
 */
@Slf4j
public class TagSlowLoopRunner extends TimerTask implements HealthIndicator {

  private static final int MAX_MINS_SINCE_SUCCESS_DEFAULT = 60;

  private final WiseTimeConnector connector;
  private final AtomicBoolean runLock = new AtomicBoolean(false);
  private final int maxMinsSinceSuccess;
  DateTime lastSuccessfulRun;

  public TagSlowLoopRunner(WiseTimeConnector connector) {
    this.connector = connector;
    this.lastSuccessfulRun = DateTime.now();
    maxMinsSinceSuccess = RuntimeConfig
        .getInt(ConnectorConfigKey.HEALTH_MAX_MINS_SINCE_SUCCESS)
        .orElse(MAX_MINS_SINCE_SUCCESS_DEFAULT);
  }

  @Override
  public void run() {
    if (runLock.compareAndSet(false, true)) {
      try {
        connector.performTagUpdateSlowLoop();
        lastSuccessfulRun = DateTime.now();
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

  @Override
  public boolean isHealthy() {
    return Minutes.minutesBetween(lastSuccessfulRun, DateTime.now()).getMinutes() < maxMinsSinceSuccess;
  }
}
