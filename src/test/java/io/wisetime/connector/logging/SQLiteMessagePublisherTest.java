/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.junit.jupiter.api.Test;

import java.io.File;

import io.wisetime.connector.datastore.CoreLocalDbTable;
import io.wisetime.connector.datastore.SQLiteHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class SQLiteMessagePublisherTest {

  private final SQLiteHelper sqLiteHelper = new SQLiteHelper(new File("test1.db"));
  private final SQLiteMessagePublisher sqLiteMessagePublisher = new SQLiteMessagePublisher(sqLiteHelper);

  @Test
  void testConstructor_createsDbTables() {
    assertThat(sqLiteHelper.doesTableExist(CoreLocalDbTable.TABLE_LOG))
        .isTrue();
    assertThat(sqLiteHelper.doesTableExist(CoreLocalDbTable.TABLE_EVENT))
        .isTrue();
  }

  @Test
  void testPublishLog_createsDbLogEntry() {
    final WtLog log = new WtLog(WtLog.Level.INFO, "Thread", "Text");
    int rowCountBefore = getRowCountInTable(CoreLocalDbTable.TABLE_LOG.getName());
    sqLiteMessagePublisher.publish(log);
    int rowCountAfter = getRowCountInTable(CoreLocalDbTable.TABLE_LOG.getName());
    assertThat(rowCountAfter)
        .isEqualTo(rowCountBefore + 1);
  }

  @Test
  void testPublishLog_createsDbEventEntry() {
    final WtEvent event = new WtEvent(WtEvent.Type.HEALTH_CHECK_FAILED);
    int rowCountBefore = getRowCountInTable(CoreLocalDbTable.TABLE_EVENT.getName());
    sqLiteMessagePublisher.publish(event);
    int rowCountAfter = getRowCountInTable(CoreLocalDbTable.TABLE_EVENT.getName());
    assertThat(rowCountAfter)
        .isEqualTo(rowCountBefore + 1);
  }

  private int getRowCountInTable(String name) {
    return sqLiteHelper.query()
        .select("SELECT COUNT(*) FROM " + name)
        .singleResult(Mappers.singleInteger());
  }
}
