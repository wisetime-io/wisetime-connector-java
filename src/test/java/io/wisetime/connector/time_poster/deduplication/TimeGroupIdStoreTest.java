/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.deduplication;

import com.github.javafaker.Faker;

import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.datastore.SQLiteHelper;

import static io.wisetime.connector.datastore.CoreLocalDbTable.TABLE_TIME_GROUPS_RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author pascal.filippi@gmail.com
 */
class TimeGroupIdStoreTest {

  private final TimeGroupIdStore timeGroupIdStore;
  private final SQLiteHelper sqLiteHelper;
  private final Faker faker = new Faker();

  TimeGroupIdStoreTest() {
    sqLiteHelper = new SQLiteHelper(new File("temp.db"));
    this.timeGroupIdStore = new TimeGroupIdStore(sqLiteHelper);
  }

  @Test
  void putAndVerifyIdFetchClient() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("persisted id should be found")
        .hasValue(PostResultStatus.SUCCESS.name());
  }

  @Test
  void putAndVerifyIdWebHook() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeenWebHook(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeenWebHook(id))
        .as("persisted id should be found")
        .usingFieldByFieldValueComparator()
        .hasValue(PostResult.SUCCESS().withMessage(""));
  }

  @Test
  void deleteOldOnInsert() {
    final String oldId = faker.numerify("tg##########");
    sqLiteHelper.query().update("INSERT INTO " + TABLE_TIME_GROUPS_RECEIVED.getName() +
        " (time_group_id, post_result, received_timestamp, created_ts, message) VALUES (?,?,?,?,?)")
        .params(oldId, PostResultStatus.SUCCESS.name(), System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8),
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8), "")
        .run();
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeenFetchClient(oldId))
        .as("old id should be found")
        .isPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeenFetchClient(oldId))
        .isNotPresent();
  }

  @Test
  void putAndDeleteId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.PERMANENT_FAILURE.name(), "");
    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("persisted id should be found")
        .hasValue(PostResultStatus.PERMANENT_FAILURE.name());

    // delete a value
    timeGroupIdStore.deleteTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("persisted id should be found")
        .isNotPresent();
  }

  @Test
  void doublePutAndVerifyId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("random id should be absent")
        .isNotPresent();

    // store a value
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.PERMANENT_FAILURE.name(), "");
    // second put should update status
    timeGroupIdStore.putTimeGroupId(id, PostResultStatus.SUCCESS.name(), "");
    assertThat(timeGroupIdStore.alreadySeenFetchClient(id))
        .as("persisted id should be found")
        .hasValue(PostResultStatus.SUCCESS.name());
  }

  @Test
  void getAllWithPendingStatusUpdate() {
    final String id1 = faker.numerify("tg##########");
    final String id2 = faker.numerify("tg##########");
    final String id3 = faker.numerify("tg##########");
    final String id4 = faker.numerify("tg##########");
    final String id5 = faker.numerify("tg##########");
    final String message1 = faker.gameOfThrones().quote();
    final String message2 = faker.gameOfThrones().quote();
    final String message3 = faker.gameOfThrones().quote();
    timeGroupIdStore.putTimeGroupId(id1, PostResultStatus.PERMANENT_FAILURE.name(), message1);
    timeGroupIdStore.putTimeGroupId(id2, PostResultStatus.SUCCESS.name(), message2);
    timeGroupIdStore.putTimeGroupId(id3, TimeGroupIdStore.IN_PROGRESS, "");
    timeGroupIdStore.putTimeGroupId(id4, TimeGroupIdStore.SUCCESS_AND_SENT, "");
    timeGroupIdStore.putTimeGroupId(id5, PostResultStatus.SUCCESS.name(), message3);

    assertThat(timeGroupIdStore.getAllWithPendingStatusUpdate())
        .usingRecursiveFieldByFieldElementComparator()
        .contains(
            Pair.of(id1, PostResult.PERMANENT_FAILURE().withMessage(message1)),
            Pair.of(id2, PostResult.SUCCESS().withMessage(message2)),
            Pair.of(id5, PostResult.SUCCESS().withMessage(message3))
        );
  }
}
