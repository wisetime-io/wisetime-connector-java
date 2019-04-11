package io.wisetime.connector.fetch_client;

import com.github.javafaker.Faker;

import org.junit.jupiter.api.Test;

import java.io.File;

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
        .isFalse();

    // store a value
    timeGroupIdStore.putTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .isTrue();
  }

  @Test
  void putAndDeleteId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("random id should be absent")
        .isFalse();

    // store a value
    timeGroupIdStore.putTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .isTrue();

    // delete a value
    timeGroupIdStore.deleteTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .isFalse();
  }

  @Test
  void doublePutAndVerifyId() {
    final String id = faker.numerify("tg##########");

    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("random id should be absent")
        .isFalse();

    // store a value
    timeGroupIdStore.putTimeGroupId(id);
    // second put should make no difference
    timeGroupIdStore.putTimeGroupId(id);
    assertThat(timeGroupIdStore.alreadySeen(id))
        .as("persisted id should be found")
        .isTrue();
  }
}
