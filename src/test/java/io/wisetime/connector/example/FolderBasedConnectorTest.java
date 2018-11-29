/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.datastore.ConnectorStore;
import io.wisetime.connector.integrate.ConnectorModule;
import io.wisetime.connector.template.TemplateFormatter;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.UpsertTagRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author thomas.haines@practiceinsight.io
 */
class FolderBasedConnectorTest {

  private final Faker faker = new Faker();
  private Path watchDir;
  private FolderBasedConnector folderConnect;
  private ApiClient apiClientMock;
  private TemplateFormatter templateFormatterMock;

  @BeforeEach
  void standUp() throws Exception {
    watchDir = Files.createTempDirectory("watch");
    folderConnect = new FolderBasedConnector(watchDir.toFile());
    apiClientMock = mock(ApiClient.class);
    templateFormatterMock = mock(TemplateFormatter.class);
    ConnectorModule connectorModuleMock = new ConnectorModule(
        apiClientMock,
        templateFormatterMock,
        mock(ConnectorStore.class));
    folderConnect.init(connectorModuleMock);

    Random randomMock = mock(Random.class);
    // default to not throwing exception
    when(randomMock.nextInt(anyInt())).thenReturn(0);
    folderConnect.setRandom(randomMock);
  }

  @Test
  void tagName() {
    assertThat(folderConnect.getTagName("foo"))
        .isEqualTo("foo");

    assertThat(folderConnect.getTagName("bar.tag"))
        .isEqualTo("bar");
  }

  @Test
  void performTagUpdate() throws IOException {
    String tagName = faker.superhero().prefix();
    String fileContents = faker.numerify("hello-world_#####");

    // create a tag file
    FileUtils.writeStringToFile(new File(watchDir.toFile(), tagName + ".tag"), fileContents, StandardCharsets.UTF_8);
    // create a skip file
    FileUtils.writeStringToFile(new File(watchDir.toFile(), tagName + ".skip"), "skip-world", StandardCharsets.UTF_8);

    // do update
    folderConnect.performTagUpdate();

    ArgumentCaptor<UpsertTagRequest> requestCaptor = ArgumentCaptor.forClass(UpsertTagRequest.class);
    verify(apiClientMock, times(1)).tagUpsert(requestCaptor.capture());

    UpsertTagRequest request = requestCaptor.getValue();
    assertThat(request.getName())
        .isEqualTo(tagName);

    assertThat(request.getPath())
        .isEqualTo("/");

    File destDir = new File(watchDir.toFile(), "uploaded");
    File destFile = new File(destDir, tagName + ".tag");
    assertThat(destFile.exists()).isTrue();

    assertThat(FileUtils.readFileToString(destFile, StandardCharsets.UTF_8))
        .as("tag is moved after calling api to add without error")
        .contains(fileContents);
  }

  @Test
  void postTime() throws Exception {
    EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .collectionSizeRange(1, 10)
        .build();

    final TimeGroup timeGroup = enhancedRandom.nextObject(TimeGroup.class);

    folderConnect.postTime(mock(spark.Request.class), timeGroup);

    File destDir = new File(watchDir.toFile(), "posted");
    File destFile = new File(destDir, timeGroup.getGroupId() + ".json");

    String jsonWritten = FileUtils.readFileToString(destFile, StandardCharsets.UTF_8);

    ObjectMapper om = folderConnect.getOm();
    TimeGroup timeGroupSaved = om.readValue(jsonWritten, TimeGroup.class);
    assertThat(timeGroupSaved)
        .as("expect deserialized is equal")
        .isEqualTo(timeGroup);

    verify(templateFormatterMock, times(1)).format(timeGroup);
  }
}
