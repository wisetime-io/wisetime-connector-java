/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.datastore;

import org.apache.commons.lang3.StringUtils;
import org.codejargon.fluentjdbc.api.FluentJdbc;
import org.codejargon.fluentjdbc.api.FluentJdbcBuilder;
import org.codejargon.fluentjdbc.api.query.Query;
import org.codejargon.fluentjdbc.api.query.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * @author thomas.haines@practiceinsight.io
 */
public class FileStore implements StorageManager {

  private static final Logger log = LoggerFactory.getLogger(FileStore.class);
  private final FluentJdbc fluentJdbc;

  /**
   * @param storageDirectoryPath The directory in which the database file should be stored.  If a null string is based, the
   *                             database will be stored in a temporary (ephemeral) directory.
   */
  public FileStore(@Nullable String storageDirectoryPath) {

    File databaseFile = getDatabasePath(storageDirectoryPath);
    log.info("database file path = {}", databaseFile.getAbsolutePath());
    SQLiteDataSource dataSource = new SQLiteDataSource();
    dataSource.setUrl(String.format("jdbc:sqlite:%s", databaseFile.getAbsolutePath()));

    fluentJdbc = new FluentJdbcBuilder()
        .connectionProvider(dataSource)
        .build();
    fluentJdbc.query()
        .update(
            "CREATE TABLE IF NOT EXISTS key_map" +
                " ( id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "   key_name TEXT, " +
                "   value TEXT " +
                " ) ")
        .run();
  }

  public Optional<String> findValue(String keyName) {
    return fluentJdbc.query()
        .select("SELECT value FROM key_map WHERE key_name=?")
        .params(keyName)
        .firstResult(rs -> rs.getString(1));
  }

  public void setValue(String keyName, String value) {
    final Query query = fluentJdbc.query();
    query.transaction().inNoResult(() -> {
      UpdateResult result = query.update("UPDATE key_map SET value=? WHERE key_name=?")
          .params(value, keyName)
          .run();
      if (result.affectedRows() == 0) {
        // new key value
        query.update("INSERT INTO key_map (key_name,value) VALUES (?,?)")
            .params(keyName, value)
            .run();
      }
    });
  }

  private File getDatabasePath(String storageDirectoryPath) {
    if (StringUtils.isBlank(storageDirectoryPath)) {
      // create temporary directory if none provided
      storageDirectoryPath = createTempDir().getAbsolutePath();
    }

    File storageDirectory = new File(storageDirectoryPath);
    if (!storageDirectory.exists()) {
      throw new IllegalArgumentException(String.format("persistent directory does not exist: '%s'",
          storageDirectory.getAbsolutePath()));
    }
    return new File(storageDirectory, "wisetime.sqlite");
  }

  private File createTempDir() {
    try {
      File tempDir = Files.createTempDirectory("wt-sqlite").toFile();
      if (!tempDir.exists()) {
        boolean mkDirResult = tempDir.mkdirs();
        if (mkDirResult) {
          log.debug("temp dir created at {}", tempDir.getAbsolutePath());
        }
      }
      return tempDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
