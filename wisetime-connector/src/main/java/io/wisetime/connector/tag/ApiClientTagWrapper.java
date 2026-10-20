/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.tag;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Wrapper for {@link ApiClient} to notify {@link TagRunner} about upload tags events.
 *
 * @author vadym
 */
@RequiredArgsConstructor
public class ApiClientTagWrapper implements ApiClient {

  @Delegate(excludes = TagUpsert.class)
  private final ApiClient apiClient;
  private final TagRunner tagRunner;

  @Override
  public void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    apiClient.tagUpsert(upsertTagRequest);
    tagRunner.onSuccessfulTagUpload();
  }

  @Override
  public void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException {
    apiClient.tagUpsertBatch(upsertTagRequests);
    tagRunner.onSuccessfulTagUpload();
  }

  @SuppressWarnings("unused")
  private interface TagUpsert {
    void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

    void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException;
  }
}
