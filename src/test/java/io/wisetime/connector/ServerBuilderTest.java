/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.wisetime.connector.ServerRunner.ServerBuilder;

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
}