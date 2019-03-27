/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.wisetime.connector.ServerRunner.ServerBuilder;
import io.wisetime.connector.logging.DisabledMessagePublisher;
import io.wisetime.connector.logging.WtTurboFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class ServerBuilderTest {

  @Test
  void createDynamicJoranConfigPath() throws IOException {
    String path = ServerBuilder.createDynamicJoranConfigPath("/logging/logback-default.xml", "");

    String result = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);

    assertThat(result)
        .as("empty config should be empty user property")
        .contains("<propertiesFilePath></propertiesFilePath>");
  }

  @Test
  void createDynamicJoranConfigWithPath() throws IOException {
    String path = ServerBuilder.createDynamicJoranConfigPath("/logging/logback-default.xml", "myPath");

    String result = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);

    assertThat(result)
        .as("use string provided")
        .contains("<propertiesFilePath>myPath</propertiesFilePath>");
  }

  @Test
  void addWtTurboFilterToLoggerContext() {
    final ServerBuilder serverBuilder = new ServerBuilder();
    serverBuilder.configureStandardLogging("invalidLogXmlResource", new WtTurboFilter(new DisabledMessagePublisher()));

    Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    LoggerContext loggerContext = rootLogger.getLoggerContext();

    assertThat(loggerContext.getTurboFilterList().size())
        .isEqualTo(1);
  }
}
