/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster;

/**
 * Dummy implementation of TimePosterRunner that is being used when time posting is disabled for this connector.
 * Use case: Connector runs to upload tags only.
 *
 * @author pascal.filippi@staff.wisetime.com
 */
public class NoOpTimePoster implements TimePoster {
  @Override
  public void start() {
    // do nothing
  }

  @Override
  public void stop() throws Exception {
    // do nothing
  }

  @Override
  public boolean isHealthy() {
    // does nothing and therefore is always healthy
    return true;
  }
}
