/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;

import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

/**
 * @author thomas.haines@practiceinsight.io
 */
public interface ApiClient {

  /**
   * Create a new tag, or update the tag if it already exists.
   */
  UpsertTagResponse tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

  /**
   * Delete an existing tag
   */
  DeleteTagResponse tagDelete(String tagName) throws IOException;


  TeamInfoResult teamInfo() throws IOException;
}
