/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class ApiClientTagWrapperTest {

  private ApiClient delegate;
  private TagRunner tagRunner;
  private ApiClientTagWrapper tagWrapper;

  @BeforeEach
  void setup() {
    delegate = mock(ApiClient.class);
    tagRunner = mock(TagRunner.class);
    tagWrapper = new ApiClientTagWrapper(delegate, tagRunner);
  }

  @Test
  void tagUpsert() throws IOException {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    tagWrapper.tagUpsert(upsertTagRequest);
    verify(tagRunner).onSuccessfulTagUpload();
  }

  @Test
  void tagUpsert_error() throws Exception {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    doThrow(new RuntimeException("API ERROR"))
        .when(delegate)
        .tagUpsert(any());

    assertThatThrownBy(() -> tagWrapper.tagUpsert(upsertTagRequest))
        .as("Metric Wrapper shouldn't handle exceptions")
        .isInstanceOf(RuntimeException.class)
        .hasMessage("API ERROR");
    verify(tagRunner, never()).onSuccessfulTagUpload();
  }

  @Test
  void tagUpsertBatch() throws IOException {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    tagWrapper.tagUpsertBatch(Collections.singletonList(upsertTagRequest));
    verify(tagRunner).onSuccessfulTagUpload();
  }

  @Test
  void tagUpsertBatch_error() throws Exception {
    UpsertTagRequest upsertTagRequest = new UpsertTagRequest();
    doThrow(new RuntimeException("API ERROR"))
        .when(delegate)
        .tagUpsertBatch(any());

    assertThatThrownBy(() -> tagWrapper.tagUpsertBatch(Collections.singletonList(upsertTagRequest)))
        .as("Metric Wrapper shouldn't handle exceptions")
        .isInstanceOf(RuntimeException.class)
        .hasMessage("API ERROR");
    verify(tagRunner, never()).onSuccessfulTagUpload();
  }
}