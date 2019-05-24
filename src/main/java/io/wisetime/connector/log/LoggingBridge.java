package io.wisetime.connector.log;

/**
 * @author thomas.haines
 */
public interface LoggingBridge {

  void writeMessage(LogEntryCW msg);

  void flushMessages();

}
