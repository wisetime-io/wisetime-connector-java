/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;

import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.AddKeywordsResponse;
import io.wisetime.generated.connect.DeleteKeywordResponse;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
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

  AddKeywordsResponse tagAddKeywords(String tagName, AddKeywordsRequest addKeywordsRequest) throws IOException;

  DeleteKeywordResponse tagDeleteKeyword(String tagName, String keyword) throws IOException;

  TeamInfoResult teamInfo() throws IOException;

  SubscribeResult postedTimeSubscribe(SubscribeRequest subscribeRequest) throws IOException;
}
