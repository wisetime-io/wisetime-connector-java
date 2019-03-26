/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import io.wisetime.connector.datastore.CoreLocalDbTable;
import io.wisetime.connector.datastore.SQLiteHelper;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class SQLiteMessagePublisher implements MessagePublisher {

  private SQLiteHelper sqLiteHelper;

  public SQLiteMessagePublisher(SQLiteHelper sqLiteHelper) {
    this.sqLiteHelper = sqLiteHelper;
    sqLiteHelper.createTable(CoreLocalDbTable.TABLE_LOG);
    sqLiteHelper.createTable(CoreLocalDbTable.TABLE_EVENT);
  }

  @Override
  public void publish(WtLog log) {
    sqLiteHelper.query()
        .update("INSERT INTO " + CoreLocalDbTable.TABLE_LOG.getName() +
            " (log_timestamp,log_level,log_thread,log_text) VALUES (?,?,?,?)")
        .params(log.getTimestamp(), log.getLevel().getNumeric(), log.getThread(), log.getText())
        .run();
  }

  @Override
  public void publish(WtEvent event) {
    sqLiteHelper.query()
        .update("INSERT INTO " + CoreLocalDbTable.TABLE_EVENT.getName() +
            " (event_timestamp,event_name,event_data) VALUES (?,?,?)")
        .params(event.getTimestamp(), event.getType().name(), event.getData())
        .run();
  }

}
