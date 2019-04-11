/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.example;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.concurrent.Callable;

import io.wisetime.connector.ConnectorRunner;
import io.wisetime.connector.ConnectorRunner.ConnectorBuilder;
import io.wisetime.connector.template.TemplateFormatter;
import io.wisetime.connector.template.TemplateFormatterConfig;
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
    ConnectorBuilder serverBuilder = ConnectorRunner.createConnectorBuilder();

    if (!StringUtils.isBlank(getApiKey())) {
      serverBuilder.withApiKey(getApiKey().trim());
    }

    if (StringUtils.isEmpty(serverBuilder.getApiKey())) {
      paramFailure("apiKey is required when default api client is used");
    }

    if (StringUtils.isBlank(getFetchClientId())) {
      paramFailure("fetchClientId is required");
    }

    final File watchDir = new File(getWatchFolder());
    if (!watchDir.isDirectory() || !watchDir.exists()) {
      paramFailure(String.format("watchFolder '%s' does not exist", getWatchFolder()));
    }

    TemplateFormatterConfig templateConfig = TemplateFormatterConfig.builder()
        .withTemplatePath(getTemplatePath())
        .withWindowsClr(isTemplateUseWinClr())
        .withMaxLength(getTemplateMaxLength())
        .build();

    // our basic connector implementation for this example
    final FolderBasedConnector folderConnector = new FolderBasedConnector(
        watchDir,
        new TemplateFormatter(templateConfig));

    ConnectorRunner runner = serverBuilder
        .useFetchClient(getFetchClientId())
        .withWiseTimeConnector(folderConnector)
        .build();

    // blocking call
    runner.start();

    return null;
  }

  private void paramFailure(String failMsg) {
    System.err.println(failMsg);
    CommandLine.call(new FolderWatchConnectApp(), "--help");
    System.exit(-1);
  }
}
