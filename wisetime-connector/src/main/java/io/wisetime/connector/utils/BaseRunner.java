/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.annotations.VisibleForTesting;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.health.HealthIndicator;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;

/**
 * Base runner that encapsulates concurrency restriction and health checks.
 *
 * @author yehor.lashkul
 */
@Slf4j
public abstract class BaseRunner extends TimerTask implements HealthIndicator {

  private static final int MAX_MINS_SINCE_SUCCESS_DEFAULT = 60;

  private final AtomicBoolean runLock = new AtomicBoolean(false);
  private final int maxMinsSinceSuccess;

  @VisibleForTesting
  public DateTime lastSuccessfulRun;

  public BaseRunner() {
    lastSuccessfulRun = DateTime.now();
    maxMinsSinceSuccess = getMaxMinsSinceSuccess();
  }

  /**
   * Invokes by a single thread at a time.
   */
  protected abstract void performAction();

  protected abstract Logger getLogger();

  @Override
  public void run() {
    if (!runLock.compareAndSet(false, true)) {
      log.info("Skip {} timer instantiation, previous process is yet to complete", getClass().getSimpleName());
      return;
    }

    try {
      performAction();
      onSuccess();
    } catch (Exception e) {
      getLogger().error(e.getMessage(), e);
    } finally {
      // ensure lock is released
      runLock.set(false);
    }
  }

  @Override
  public boolean isHealthy() {
    return Minutes.minutesBetween(lastSuccessfulRun, DateTime.now()).getMinutes() < maxMinsSinceSuccess;
  }

  protected void onSuccess() {
    lastSuccessfulRun = DateTime.now();
  }

  protected int getMaxMinsSinceSuccess() {
    return RuntimeConfig
        .getInt(ConnectorConfigKey.HEALTH_MAX_MINS_SINCE_SUCCESS)
        .orElse(MAX_MINS_SINCE_SUCCESS_DEFAULT);
  }
}
