/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.activity_type;

/**
 * @author yehor.lashkul
 */
public class NoOpActivityTypeRunner extends ActivityTypeRunner {

  public NoOpActivityTypeRunner() {
    super(null);
  }

  @Override
  public void run() {
    // no action
  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
