/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class DisabledMessagePublisher implements MessagePublisher {

  @Override
  public void publish(WtEvent event) {

  }

  @Override
  public void publish(WtLog log) {

  }
}
