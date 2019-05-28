package io.wisetime.connector.log;

/**
 * @author thomas.haines
 */
public interface LoggingBridge {

  void writeMessage(LogQueueCW.LogEntryCW msg);

  void flushMessages();

}
