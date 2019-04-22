/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import org.apache.commons.lang3.StringUtils;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Query;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.sql.DataSource;

import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;

import static io.wisetime.connector.config.ConnectorConfigKey.DATA_DIR;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class SQLiteHelper {

  private static final String JDBC_DRIVER = "jdbc:sqlite:%s?journal_mode=WAL&synchronous=OFF&journal_size_limit=500";

  private static final String DEFAULT_LOCAL_DB_FILENAME = "wisetime.sqlite";
  private static final String DEFAULT_TEMP_DIR_NAME = "wt-sqlite";

  private FluentJdbc fluentJdbc;

  public SQLiteHelper(File databaseFile) {
    setupDataSource(databaseFile);
  }

  public SQLiteHelper(boolean persistentStorageOnly) {
    final String persistentStoreDirPath = RuntimeConfig.getString(DATA_DIR).orElse(null);
    if (persistentStorageOnly && StringUtils.isBlank(persistentStoreDirPath)) {
      throw new IllegalArgumentException(String.format(
          "requirePersistentStore enabled for server -> a persistent directory must be provided using setting '%s'",
          ConnectorConfigKey.DATA_DIR.getConfigKey()));
    }

    final File persistentStoreDir = new File(StringUtils.isNotBlank(persistentStoreDirPath) ?
        persistentStoreDirPath : createTempDir().getAbsolutePath());
    if (!persistentStoreDir.exists() && !persistentStoreDir.mkdirs()) {
      throw new IllegalArgumentException(String.format("Store directory does not exist: '%s'",
          persistentStoreDir.getAbsolutePath()));
    }
    setupDataSource(new File(persistentStoreDir, DEFAULT_LOCAL_DB_FILENAME));
  }

  private File createTempDir() {
    try {
      File tempDir = Files.createTempDirectory(DEFAULT_TEMP_DIR_NAME).toFile();
      if (!tempDir.exists()) {
        boolean mkDirResult = tempDir.mkdirs();
        if (mkDirResult) {
          // log.debug("temp dir created at {}", tempDir.getAbsolutePath());
        }
      }
      return tempDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void setupDataSource(File databaseFile) {
    this.fluentJdbc = new FluentJdbcBuilder()
        .connectionProvider(fileToDataSource(databaseFile))
        .build();
  }

  private DataSource fileToDataSource(File databaseFile) {
    final SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
    sqLiteDataSource.setUrl(String.format(JDBC_DRIVER, databaseFile.getAbsolutePath()));
    return sqLiteDataSource;
  }

  public boolean createTable(LocalDbTable table) {
    fluentJdbc.query()
        .update("CREATE TABLE IF NOT EXISTS " + table.getName() + " ( " + table.getSchema() + " ) ")
        .run();
    return doesTableExist(table);
  }

  private boolean doesTableExist(LocalDbTable table) {
    int foundTables = fluentJdbc.query()
        .select("SELECT * FROM sqlite_master WHERE type='table' and name='" + table.getName() + "'")
        .listResult(Mappers.map())
        .size();
    return foundTables >= 1;
  }

  public Query query() {
    return fluentJdbc.query();
  }
}
