package io.wisetime.wise_log_aws.cloud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WiseAppenderTest {

  @Test
  @DisplayName("WiseAppender with mocked LogsWriter")
  public void testWiseAppender() {
    CloudWriter mockedCloudWriter = mock(CloudWriter.class);

    Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    LoggerContext loggerContext = rootLogger.getLoggerContext();
    loggerContext.reset();

    WiseAppender awsAppender = createWiseAppender(mockedCloudWriter, loggerContext);

    rootLogger.addAppender(awsAppender);

    rootLogger.trace("Message 1");
    rootLogger.warn("Message 2");
    rootLogger.warn("More messages");
    rootLogger.error("More messages", new Exception("error created for test"));

    verify(mockedCloudWriter, times(3)).addMessageToQueue(any());
    verify(mockedCloudWriter, never()).processLogEntries(); // because WiseAppender has a queue

    awsAppender.stop(); // stop fires flushing

    verify(mockedCloudWriter, times(1)).processLogEntries();
    verify(mockedCloudWriter, times(1)).stop();
  }

  private WiseAppender createWiseAppender(CloudWriter cloudWriter, LoggerContext loggerContext) {
    WiseAppender awsAppender = new WiseAppender();
    awsAppender.setContext(loggerContext);
    awsAppender.setModuleName("fooModule");
    awsAppender.setLogGroup("config_miss_group_name");
    awsAppender.start();
    awsAppender.setLogsWriter(cloudWriter);
    awsAppender.reEnableForTest();
    return awsAppender;
  }
}
