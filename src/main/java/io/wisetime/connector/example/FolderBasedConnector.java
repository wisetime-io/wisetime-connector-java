/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.example;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.template.TemplateFormatter;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.UpsertTagRequest;
import spark.Request;

/**
 * An example connector that uses the creation of files as a trigger to create a corresponding tag a given team.
 *
 * @author thomas.haines@practiceinsight.io
 */
class FolderBasedConnector implements WiseTimeConnector {

  private static final Logger log = LoggerFactory.getLogger(FolderBasedConnector.class);
  private static final String TAG_SUFFIX = ".tag";
  private final ObjectMapper om = TolerantObjectMapper.create();
  private Random random = new Random();
  private final File uploadedDir;
  private final File postDir;
  private final File watchDir;
  private final String callerKey;
  private ApiClient apiClient;
  private TemplateFormatter publicTemplateFormatter;
  private TemplateFormatter internalTemplateFormatter;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  FolderBasedConnector(File watchDir, String callerKey) {
    this.watchDir = watchDir;
    if (!watchDir.exists() || !watchDir.isDirectory()) {
      throw new IllegalStateException(String.format("invalid directory '%s'", watchDir.getAbsolutePath()));
    }
    this.callerKey = callerKey;
    if (StringUtils.isBlank(callerKey)) {
      throw new IllegalArgumentException("Caller key must be provided");
    }

    postDir = new File(watchDir, "posted");
    if (!postDir.exists()) {
      postDir.mkdir();
    }

    uploadedDir = new File(watchDir, "uploaded");
    if (!uploadedDir.exists()) {
      uploadedDir.mkdir();
    }
  }

  @Override
  public void init(ConnectorModule connectorModule) {
    this.apiClient = connectorModule.getApiClient();
    this.publicTemplateFormatter = connectorModule.getPublicTemplateFormatter();
    this.internalTemplateFormatter = connectorModule.getInternalTemplateFormatter();
  }

  /**
   * The example folder connector search a watch directory for any files with the suffix '.tag', and when found, creates a
   * tag with the name of the file (without the .tag suffix).  The contents of the file are ignored.  On successfuly upload,
   * the file is moved into an `uploaded` sub-directory of the watch directory.
   */
  @Override
  public void performTagUpdate() {
    // get list of files in watch dir ending in ".tag"
    File[] resultList = watchDir.listFiles(this::isNewTag);
    if (resultList == null || resultList.length == 0) {
      log.info("No new tags detected");
      return;
    }
    List<File> newTagList = Lists.newArrayList(resultList);
    for (File tagFile : newTagList) {
      saveTag(tagFile);
    }
  }

  private boolean isNewTag(File dir, String name) {
    if (name.toLowerCase().length() < 5 || !name.toLowerCase().endsWith(TAG_SUFFIX)) {
      return false;
    }
    File result = new File(dir, name);
    return !result.isDirectory();
  }

  private void saveTag(File file) {
    String trimmedTag = getTagName(file.getName());

    File renameTo = new File(uploadedDir, file.getName());
    if (renameTo.exists()) {
      boolean resulted = file.delete();
      log.info("file deleted={} as existing {}", resulted, trimmedTag);
      return;
    }

    UpsertTagRequest request = new UpsertTagRequest()
        .name(trimmedTag)
        .path("/");

    try {
      apiClient.tagUpsert(request);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    boolean result = file.renameTo(renameTo);
    log.info("file renamed={} for tag to file {}", result, renameTo.getName());
  }

  String getTagName(String fileName) {
    if (!fileName.toLowerCase().endsWith(TAG_SUFFIX)) {
      return fileName;
    }
    return fileName.substring(0, fileName.length() - TAG_SUFFIX.length());
  }

  /**
   * The folder watcher implementation saves a JSON representation of TimeGroup into a sub-directory `posted`.
   */
  @Override
  public PostResult postTime(Request request, TimeGroup userPostedTime) {
    try {
      // chaos monkey
      if (random.nextInt(10) == 1) {
        return PostResult.TRANSIENT_FAILURE;
      }
      // Wrong caller key
      if (!callerKey.equals(userPostedTime.getCallerKey())) {
        return PostResult.PERMANENT_FAILURE;
      }

      String resultAsJson = om.writeValueAsString(userPostedTime);
      File resultFile = new File(postDir, userPostedTime.getGroupId() + ".json");
      FileUtils.writeStringToFile(resultFile, resultAsJson, StandardCharsets.UTF_8);

      String humanReadableDescription = publicTemplateFormatter.format(userPostedTime);
      File descriptionFile = new File(watchDir, userPostedTime.getGroupId() + ".txt");
      FileUtils.writeStringToFile(descriptionFile, humanReadableDescription, StandardCharsets.UTF_8);

      String internalDescription = internalTemplateFormatter.format(userPostedTime);
      File internalDescriptionFile = new File(watchDir, userPostedTime.getGroupId() + "-internal.txt");
      FileUtils.writeStringToFile(internalDescriptionFile, internalDescription, StandardCharsets.UTF_8);

      return PostResult.SUCCESS;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // visible for testing
  FolderBasedConnector setRandom(Random random) {
    this.random = random;
    return this;
  }

  ObjectMapper getOm() {
    return om;
  }
}
