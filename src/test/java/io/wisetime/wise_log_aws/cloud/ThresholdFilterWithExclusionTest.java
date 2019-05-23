package io.wisetime.wise_log_aws.cloud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import io.wisetime.connector.logging.aws.ThresholdFilterWithExclusion;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdFilterWithExclusionTest {

  @Test
  @DisplayName("test decide - filter by level, excluded log prefix should not be taken into account")
  void decide() {
    String excludedLogPrefixTSV = "io.abc;" + this.getClass().getPackage().getName();
    // filter WARN, ERROR except from loggers named excludedLogPrefix
    ThresholdFilterWithExclusion filter = new ThresholdFilterWithExclusion();
    filter.setLevel("WARN");
    filter.setExcludedLogPrefixList(excludedLogPrefixTSV);
    filter.start();

    FilterReply reply = filter.decide(createLogEvent(Level.DEBUG, this.getClass().getName()));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.DEBUG, this.getClass().getName()));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.DEBUG, "io.abc.XYZ"));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.WARN, this.getClass().getName()));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.WARN, this.getClass().getName()));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.WARN, "someLogger"));
    assertThat(reply).isEqualTo(FilterReply.NEUTRAL);

    reply = filter.decide(createLogEvent(Level.INFO, "someLogger"));
    assertThat(reply).isEqualTo(FilterReply.DENY);
  }

  private LoggingEvent createLogEvent(Level level, String loggerName) {
    Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
    LoggingEvent event = new LoggingEvent(null, logger, level, "msg...", new Exception("Some exception"), null);
    return event;
  }
}
