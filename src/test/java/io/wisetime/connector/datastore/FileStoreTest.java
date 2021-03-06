/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javafaker.Faker;
import java.io.File;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
 * @author shane.xie@practiceinsight.io
 */
class FileStoreTest {

  private final FileStore fileStore;
  private final Faker faker = new Faker();

  FileStoreTest() {
    final SqLiteHelper sqLiteHelper = new SqLiteHelper(new File("temp.db"));
    this.fileStore = new FileStore(sqLiteHelper);
  }

  @Test
  void putAndGetString() {
    final String key = randomKey();
    final String valueA = faker.gameOfThrones().quote();

    assertThat(fileStore.getString(key))
        .as("random key should be absent")
        .isNotPresent();

    // store a value
    fileStore.putString(key, valueA);
    assertThat(fileStore.getString(key))
        .as("persisted value A should be found")
        .contains(valueA);

    // update to new value
    final String valueB = faker.chuckNorris().fact();
    fileStore.putString(key, valueB);

    assertThat(fileStore.getString(key))
        .as("value updated to B should match")
        .contains(valueB);
  }

  @Test
  void putAndGetLong() {
    final String key = randomKey();
    final Long value = faker.number().randomNumber();
    fileStore.putLong(key, value);
    assertThat(fileStore.getLong(key))
        .as("we should get back the persisted value")
        .isEqualTo(Optional.of(value));
  }

  @Test
  void getLong_not_a_number() {
    final String key = randomKey();
    fileStore.putString(key, "not a number");
    assertThat(fileStore.getLong(key))
        .as("value is not a valid long")
        .isEmpty();
  }

  private String randomKey() {
    return faker.numerify("key##########");
  }
}
