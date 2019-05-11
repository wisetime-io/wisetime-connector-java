/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.fetch_client;

import com.github.javafaker.Faker;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.datastore.SQLiteHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author pascal.filippi@gmail.com
 */
class TimeGroupIdStoreTest {

  private final TimeGroupIdStore timeGroupIdStore;
  private final Faker faker = new Faker();

  TimeGroupIdStoreTest() {
    final SQLiteHelper sqLiteHelper = new SQLiteHelper(new File("temp.db"));
    this.timeGroupIdStore = new TimeGroupIdStore(sqLiteHelper);
  }

  @Test
  void putAndVerifyId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResult.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .hasValue(PostResult.SUCCESS.name());
  }

  @Test
  void putAndDeleteId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResult.PERMANENT_FAILURE.name(), "");
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .hasValue(PostResult.PERMANENT_FAILURE.name());

    // delete a value
    timeGroupIdStore.deleteTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .isNotPresent();
  }

  @Test
  void doublePutAndVerifyId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResult.PERMANENT_FAILURE.name(), "");
    // second put should update status
    timeGroupIdStore.putTimeGroupId(id, PostResult.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .hasValue(PostResult.SUCCESS.name());
  }

  @Test
  void getAllWithPendingStatusUpdate() {
    final String id1 = faker.numerify("tg##########");
    final String id2 = faker.numerify("tg##########");
    final String id3 = faker.numerify("tg##########");
    final String id4 = faker.numerify("tg##########");
    final String message1 = faker.gameOfThrones().quote();
    final String message2 = faker.gameOfThrones().quote();
    timeGroupIdStore.putTimeGroupId(id1, PostResult.PERMANENT_FAILURE.name(), message1);
    timeGroupIdStore.putTimeGroupId(id2, PostResult.SUCCESS.name(), message2);
    timeGroupIdStore.putTimeGroupId(id3, TimeGroupIdStore.IN_PROGRESS, "");
    timeGroupIdStore.putTimeGroupId(id4, TimeGroupIdStore.SUCCESS_AND_SENT, "");

    assertThat(timeGroupIdStore.getAllWithPendingStatusUpdate())
        .usingFieldByFieldElementComparator()
        .contains(
            Pair.of(id1, PostResult.PERMANENT_FAILURE.withMessage(message1)),
            Pair.of(id2, PostResult.SUCCESS.withMessage(message2))
        );
  }
}
