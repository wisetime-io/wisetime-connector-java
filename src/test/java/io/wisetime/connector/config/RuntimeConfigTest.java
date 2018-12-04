/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import io.wisetime.connector.test_util.FakerUtil;

import static io.wisetime.connector.config.ConnectorConfigKey.CONNECTOR_PROPERTIES_FILE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings({"WeakerAccess", "ResultOfMethodCallIgnored"})
public class RuntimeConfigTest {

  static FakerUtil faker;

  @BeforeAll
  public static void setup() {
    faker = new FakerUtil();
  }

  @Test
  void testUserPropertyMapFile() throws IOException {
    File propertyFile = Files.createTempFile("test_user", ".properties").toFile();
    StringBuilder fileContent = new StringBuilder();
    String keyA = faker.getRandom("key");
    String keyB = faker.getRandom("key");

    String valueA = faker.getRandom("val");
    String valueB = faker.getRandom("val");

    fileContent.append(String.format("%s=%s%n", keyA, valueA));
    fileContent.append(String.format("%s=%s%n", keyB, valueB));

    FileUtils.writeStringToFile(propertyFile, fileContent.toString(), StandardCharsets.UTF_8);

    try {
      System.setProperty(CONNECTOR_PROPERTIES_FILE.getConfigKey(), propertyFile.getPath());
      RuntimeConfig.rebuild();

      assertThat(RuntimeConfig.getString(() -> keyA))
          .contains(valueA);

      assertThat(RuntimeConfig.getString(() -> keyB))
          .contains(valueB);

    } finally {
      System.clearProperty(CONNECTOR_PROPERTIES_FILE.getConfigKey());
      RuntimeConfig.rebuild();
    }

    assertThat(RuntimeConfig.getString(() -> keyB))
        .isNotPresent();
  }

  @Test
  void getString() {
    String key = faker.faker().ancient().god();
    String value = faker.faker().ancient().titan();
    System.setProperty(key, value);

    RuntimeConfig.rebuild();
    assertThat(RuntimeConfig.getString(() -> key))
        .contains(value);
    assertThat(RuntimeConfig.getString(() -> value))
        .isNotPresent();

    System.clearProperty(key);
  }

  @Test
  void getInt() {
    String key = faker.faker().ancient().god();
    Integer value = faker.faker().number().numberBetween(10, 30);
    System.setProperty(key, value.toString());
    RuntimeConfig.rebuild();

    assertThat(RuntimeConfig.getInt(() -> key))
        .contains(value);
    assertThat(RuntimeConfig.getInt(value::toString))
        .isNotPresent();

    System.clearProperty(key);
  }
}
