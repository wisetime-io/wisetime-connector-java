/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Query;
import org.sqlite.SQLiteDataSource;

import java.io.File;

import javax.sql.DataSource;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class SQLiteHelper {

  private static final String JDBC_DRIVER = "jdbc:sqlite:%s?journal_mode=WAL&synchronous=OFF&journal_size_limit=500";

  private static DataSource fileToDataSource(File databaseFile) {
    final SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
    sqLiteDataSource.setUrl(String.format(JDBC_DRIVER, databaseFile.getAbsolutePath()));
    return sqLiteDataSource;
  }

  private FluentJdbc fluentJdbc;

  public SQLiteHelper() {
  }

  public SQLiteHelper(File databaseFile) {
    setupDataSource(databaseFile);
  }

  public void setupDataSource(File databaseFile) {
    this.fluentJdbc = new FluentJdbcBuilder()
        .connectionProvider(fileToDataSource(databaseFile))
        .build();
  }

  public boolean doesTableExist(LocalDbTable table) {
    int foundTables = fluentJdbc.query()
        .select("SELECT * FROM sqlite_master WHERE type='table' and name='" + table.getName() + "'")
        .listResult(Mappers.map())
        .size();
    return foundTables >= 1;
  }

  public boolean createTable(LocalDbTable table) {
    fluentJdbc.query()
        .update("CREATE TABLE IF NOT EXISTS " + table.getName() + " ( " + table.getSchema() + " ) ")
        .run();
    return doesTableExist(table);
  }

  public Query query() {
    return fluentJdbc.query();
  }
}
