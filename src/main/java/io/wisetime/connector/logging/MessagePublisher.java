/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public interface MessagePublisher {

  void publish(WtEvent event);

  void publish(WtLog log);

}
