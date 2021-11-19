/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

/**
 * @author thomas.haines
 */
public interface LoggingBridge {

  void writeMessage(LogQueueCW.LogEntryCW msg);

  void flushMessages();

}
