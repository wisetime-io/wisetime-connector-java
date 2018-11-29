/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;
import java.util.List;

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
   *
   * @param upsertTagRequest information about the tag to be upserted
   * @return result of the tag upsert operation
   * @throws IOException
   */
  UpsertTagResponse tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

  /**
   * Upsert a batch of tags. Use this method if you have a large number of tags to upsert.
   * Blocks until completion or throws an IOException on the first error.
   * It is safe to retry on error since tag upsert is idempotent.
   *
   * @param upsertTagRequests request list of tags to be upserted
   * @throws IOException
   */
  void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException;

  /**
   * Delete an existing tag.
   *
   * @param tagName the name of the tag to delete
   * @return result of the tag deletion operation
   * @throws IOException
   */
  DeleteTagResponse tagDelete(String tagName) throws IOException;

  /**
   * Add keywords to a tag. Existing keywords not be overwritten.
   *
   * @param tagName the tag to which to add the keywords
   * @param addKeywordsRequest request contains list of keywords to be added
   * @return result of the add keywords operation
   * @throws IOException
   */
  AddKeywordsResponse tagAddKeywords(String tagName, AddKeywordsRequest addKeywordsRequest) throws IOException;

  /**
   * Delete keyword from a tag
   *
   * @param tagName the tag whose keyword we want to delete
   * @param keyword the keyword to be deleted
   * @return result of the keyword delete operation
   * @throws IOException
   */
  DeleteKeywordResponse tagDeleteKeyword(String tagName, String keyword) throws IOException;

  /**
   * Get the details for the team linked to the API key making the request.
   *
   * @return the team information
   * @throws IOException
   */
  TeamInfoResult teamInfo() throws IOException;

  SubscribeResult postedTimeSubscribe(SubscribeRequest subscribeRequest) throws IOException;
}
