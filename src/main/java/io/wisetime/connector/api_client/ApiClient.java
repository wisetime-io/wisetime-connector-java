/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;

/**
 * Client that is responsible to perform authentication and send requests to WiseTime servers. Contains list of API methods
 * available for use.
 *
 * @author thomas.haines@practiceinsight.io
 * @see DefaultApiClient for sample implementation
 */
public interface ApiClient {

  /**
   * Create a new tag, or update the tag if it already exists.
   *
   * @param upsertTagRequest information about the tag to be upserted
   * @throws IOException
   */
  void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

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
   * @throws IOException
   */
  void tagDelete(String tagName) throws IOException;

  /**
   * Add keywords to a tag. Existing keywords will not be overwritten.
   *
   * @param tagName the tag to which to add the keywords
   * @param additionalKeywords set of new keyword Strings to add to the tag
   * @throws IOException
   */
  void tagAddKeywords(String tagName, Set<String> additionalKeywords) throws IOException;

  /**
   * Add keywords to a batch of tags. Existing keywords will not be overwritten.
   *
   * @param tagNamesAndAdditionalKeywords a map of tag names and set of keywords to add to each tag
   * @throws IOException
   */
  void tagAddKeywordsBatch(Map<String, Set<String>> tagNamesAndAdditionalKeywords) throws IOException;

  /**
   * Delete keyword from a tag.
   *
   * @param tagName the tag whose keyword we want to delete
   * @param keyword the keyword to be deleted
   * @return result of the keyword delete operation
   * @throws IOException
   */
  void tagDeleteKeyword(String tagName, String keyword) throws IOException;

  /**
   * Get the details for the team linked to the API key making the request.
   *
   * @return the team information
   * @throws IOException
   */
  TeamInfoResult teamInfo() throws IOException;

  /**
   * Subscribes a new webhook for receiving posted time. WiseTime will call your webhook whenever
   * a user posts time to your team.
   *
   * @param subscribeRequest information about the webhook to be created
   * @return the subscription result will contain the webhook ID that was assigned to the new webhook
   * @throws IOException
   */
  SubscribeResult postedTimeSubscribe(SubscribeRequest subscribeRequest) throws IOException;
}
