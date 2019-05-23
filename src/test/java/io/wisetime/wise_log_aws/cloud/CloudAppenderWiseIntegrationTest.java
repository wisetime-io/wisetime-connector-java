/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * @author thomas.haines@practiceinsight.io
 */
class CloudAppenderWiseIntegrationTest {

  @Test
  @AWSCredentialsRequired
  @DisplayName("integration test for WiseAppender")
  void testAppender() throws Exception {
    Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    LoggerContext loggerContext = rootLogger.getLoggerContext();
    loggerContext.reset();

    WiseAppender awsAppender = new WiseAppender();
    awsAppender.setContext(loggerContext);
    awsAppender.setModuleName("fooModule");
    awsAppender.setLogDefaultGroup("config_miss_group_name");
    awsAppender.start();

    // skip need for CLOUD_LOG_ENABLED=true
    awsAppender.reEnableForTest();

    rootLogger.addAppender(awsAppender);

    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%-5level [%thread]: %message%n");
    encoder.start();

    ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
    consoleAppender.setContext(loggerContext);
    consoleAppender.setEncoder(encoder);
    ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel("INFO");
    filter.start();

    consoleAppender.addFilter(filter);
    consoleAppender.start();

    rootLogger.addAppender(consoleAppender);

    rootLogger.trace("Message 1");
    rootLogger.warn("Message 2");
    rootLogger.warn("More messages");
    rootLogger.error("More messages", new Exception("doh"));

    awsAppender.stop();

    Thread.sleep(2000);
  }
}

