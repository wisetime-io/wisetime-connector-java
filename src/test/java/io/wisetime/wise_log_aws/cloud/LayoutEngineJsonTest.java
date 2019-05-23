package io.wisetime.wise_log_aws.cloud;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutEngineJsonTest {

  @Test
  void testDoLayout() throws JSONException {
    LayoutEngineJson layoutEngineJson = new LayoutEngineJson();
    Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    Level level = Level.INFO;
    String message = "This is an error message...";
    MDC.put("mdc-key1", "mdc-value1");
    MDC.put("mdc-key2", "mdc-value2");

    LoggingEvent event = new LoggingEvent(null, rootLogger, level, message, new Exception("Some exception"), null);
    layoutEngineJson.start();
    String output = layoutEngineJson.doLayout(event);

    String jsonLayoutResource = TestFileUtil.getFile("/testJsonLayout.json");


    assertThat(jsonLayoutResource)
        .isNotNull();

    JSONAssert.assertEquals(jsonLayoutResource, output, false);
    // strict mode false, because stacktrace is not predictable (depends on a test runner maven vs intellij)
  }

}
