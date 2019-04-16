package io.wisetime.connector.fetch_client;

import com.github.javafaker.Faker;

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
    timeGroupIdStore.putTimeGroupId(id, PostResult.SUCCESS.name());
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
    timeGroupIdStore.putTimeGroupId(id, PostResult.PERMANENT_FAILURE.name());
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
    timeGroupIdStore.putTimeGroupId(id, PostResult.PERMANENT_FAILURE.name());
    // second put should update status
    timeGroupIdStore.putTimeGroupId(id, PostResult.SUCCESS.name());
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .hasValue(PostResult.SUCCESS.name());
  }
}
