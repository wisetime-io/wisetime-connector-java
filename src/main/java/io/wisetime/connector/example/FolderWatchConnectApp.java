/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.example;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.concurrent.Callable;

import io.wisetime.connector.ServerRunner;
import io.wisetime.connector.ServerRunner.ServerBuilder;
import picocli.CommandLine;

/**
 * Monitors a folder for tags.
 *
 * @author thomas.haines@practiceinsight.io
 */
@CommandLine.Command(description = "A sample connector app that uses the wisetime-connector utility service",
    name = "folder-watch-example", mixinStandardHelpOptions = true, version = "1.0")
public class FolderWatchConnectApp extends FolderWatchParam implements Callable<Void> {

  public static void main(String... args) {
    CommandLine.call(new FolderWatchConnectApp(), args);
  }

  @Override
  public Void call() throws Exception {
    ServerBuilder serverBuilder = ServerRunner.createServerBuilder();

    if (!StringUtils.isBlank(getApiKey())) {
      serverBuilder.withApiKey(getApiKey().trim());
    }

    if (StringUtils.isEmpty(serverBuilder.getApiKey())) {
      paramFailure("apiKey is required when default api client is used");
    }

    if (StringUtils.isBlank(getCallerKey())) {
      paramFailure("caller key is required");
    }

    final File watchDir = new File(getWatchFolder());
    if (!watchDir.isDirectory() || !watchDir.exists()) {
      paramFailure(String.format("watchFolder '%s' does not exist", getWatchFolder()));
    }

    // our basic connector implementation for this example
    final FolderBasedConnector folderConnector = new FolderBasedConnector(watchDir, getCallerKey());

    ServerRunner runner = serverBuilder
        .withWiseTimeConnector(folderConnector)
        .withTemplateMaxLength(getTemplateMaxLength())
        .withTemplatePath(getTemplatePath())
        .withTemplateUseWinClr(isTemplateUseWinClr())
        .build();

    // blocking call
    runner.startServer();

    return null;
  }

  private void paramFailure(String failMsg) {
    System.err.println(failMsg);
    CommandLine.call(new FolderWatchConnectApp(), "--help");
    System.exit(-1);
  }
}
