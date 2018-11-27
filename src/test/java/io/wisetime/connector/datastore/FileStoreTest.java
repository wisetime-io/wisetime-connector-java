/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import com.github.javafaker.Faker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class FileStoreTest {

  private final Faker faker = new Faker();

  @Test
  void setAndFindTest() {
    FileStore fileStore = new FileStore(null);
    String key = faker.numerify("key##########");
    String valueA = faker.numerify("value##########");

    assertThat(fileStore.findValue(key))
        .as("random key should be absent")
        .isNotPresent();

    // persist key and value
    fileStore.setValue(key, valueA);

    assertThat(fileStore.findValue(key))
        .as("persisted value A should be found")
        .contains(valueA);

    // update to new value
    String valueB = faker.numerify("value##########");
    fileStore.setValue(key, valueB);

    assertThat(fileStore.findValue(key))
        .as("value updated to B should match")
        .contains(valueB);

  }
}
