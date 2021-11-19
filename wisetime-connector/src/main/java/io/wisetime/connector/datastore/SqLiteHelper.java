/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import static io.wisetime.connector.config.ConnectorConfigKey.DATA_DIR;

import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.mapper.Mappers;
import org.codejargon.fluentjdbc.api.query.Query;
import org.sqlite.SQLiteDataSource;

/**
 * @author galya.bogdanova
 */
@Slf4j
public class SqLiteHelper {

  private final FluentJdbc fluentJdbc;

  public SqLiteHelper(File databaseFile) {
    this.fluentJdbc = setupDataSource(databaseFile);
  }

  /**
   * @param persistentStorageOnly TODO(Dev) Improve explanation of use case for not using location by convention if no
   *                              explicit location is provided via config.
   */
  public SqLiteHelper(boolean persistentStorageOnly) {
    final String persistentStoreDirPath = RuntimeConfig.getString(DATA_DIR).orElse(null);
    if (persistentStorageOnly && StringUtils.isBlank(persistentStoreDirPath)) {
      throw new IllegalArgumentException(String.format(
          "requirePersistentStore enabled for server -> a persistent directory must be provided using setting '%s'",
          ConnectorConfigKey.DATA_DIR.getConfigKey()));
    }

    final File persistentStoreDir = getPersistentStorageDir(persistentStoreDirPath);
    if (!persistentStoreDir.exists() && !persistentStoreDir.mkdirs()) {
      throw new IllegalArgumentException(
          String.format("Store directory does not exist: '%s'", persistentStoreDir.getAbsolutePath()));
    }
    this.fluentJdbc = setupDataSource(new File(persistentStoreDir, "wisetime.sqlite"));
  }

  private File getPersistentStorageDir(String persistentStoreDirPath) {
    if (StringUtils.isNotBlank(persistentStoreDirPath)) {
      return new File(persistentStoreDirPath.trim());
    }

    File tempDir = createTempDir();
    log.info("using temporary storage directory {}", tempDir.getAbsolutePath());
    return tempDir;
  }

  private File createTempDir() {
    try {
      File tempDir = Files.createTempDirectory("wt-sqlite").toFile();
      if (!tempDir.exists()) {
        if (!tempDir.mkdirs()) {
          throw new IllegalStateException("Failed to create data directory: " + tempDir);
        }
      }
      return tempDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private FluentJdbc setupDataSource(File databaseFile) {
    return new FluentJdbcBuilder()
        .connectionProvider(fileToDataSource(databaseFile))
        .build();
  }

  private DataSource fileToDataSource(File databaseFile) {
    final SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
    String jdbcUrl = "jdbc:sqlite:%s?journal_mode=WAL&synchronous=OFF&journal_size_limit=500";
    sqLiteDataSource.setUrl(String.format(jdbcUrl, databaseFile.getAbsolutePath()));
    return sqLiteDataSource;
  }

  @SuppressWarnings("UnusedReturnValue")
  public boolean createTable(LocalDbTable table) {
    fluentJdbc.query()
        .update("CREATE TABLE IF NOT EXISTS " + table.getName() + " ( " + table.getSchema() + " ) ")
        .run();
    for (LocalDbTable.Modification modification : table.getModifications()) {
      boolean alreadyPresent = fluentJdbc.query()
          .select("PRAGMA table_info(" + table.getName() + ")")
          .listResult(rs -> rs.getString("name"))
          .stream()
          .anyMatch(columnName -> modification.getColumnName().equalsIgnoreCase(columnName));
      if (!alreadyPresent) {
        fluentJdbc.query().update(modification.getSql()).run();
      }
    }
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
