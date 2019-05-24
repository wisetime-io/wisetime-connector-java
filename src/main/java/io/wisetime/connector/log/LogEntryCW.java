package io.wisetime.connector.log;

import com.amazonaws.services.logs.model.InputLogEvent;

import ch.qos.logback.classic.Level;
import lombok.Data;

/**
 * @author thomas.haines
 */
@Data
class LogEntryCW {

  private final Level level;
  private final String loggerName;
  private final InputLogEvent inputLogEvent;

}
