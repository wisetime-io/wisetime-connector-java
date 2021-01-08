/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteKeywordResponse;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.SyncActivityTypesRequest;
import io.wisetime.generated.connect.SyncActivityTypesResponse;
import io.wisetime.generated.connect.SyncSession;
import io.wisetime.generated.connect.TagMetadataDeleteRequest;
import io.wisetime.generated.connect.TagMetadataDeleteResponse;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UnsubscribeRequest;
import io.wisetime.generated.connect.UnsubscribeResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.util.List;

/**
 * Client that is responsible to perform authentication and send requests to the WiseTime Connect web API. Contains a
 * list of API methods available for use.
 * <p>
 * For default implementation see {@link DefaultApiClient}.
 *
 * @author thomas.haines
 */
@SuppressWarnings("JavaDoc")
public interface ApiClient {

  /**
   * Create a new tag, or update the tag if it already exists.
   *
   * @param upsertTagRequest information about the tag to be upserted
   * @throws IOException The {@link IOException}
   */
  void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException;

  /**
   * Upsert a batch of tags. Use this method if you have a large number of tags to upsert.
   * <p>
   * Blocks until completion or throws an {@link IOException} on the first error. It is safe to retry on error since tag
   * upsert is idempotent.
   *
   * @param upsertTagRequests request list of tags to be upserted
   * @throws IOException The {@link IOException}
   */
  void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException;

  /**
   * Delete an existing tag.
   *
   * @param deleteTagRequest contains info about the tag to be deleted
   * @throws IOException The {@link IOException}
   */
  void tagDelete(DeleteTagRequest deleteTagRequest) throws IOException;

  /**
   * Add keywords to a tag. Existing keywords will not be overwritten.
   *
   * @param addKeywordsRequest contains info about the keywords to be added to a tag
   * @throws IOException The {@link IOException}
   */
  void tagAddKeywords(AddKeywordsRequest addKeywordsRequest) throws IOException;

  /**
   * Add keywords to a batch of tags. Existing keywords will not be overwritten.
   *
   * @param addKeywordsRequests request list keywords to be added to tag(s)
   * @throws IOException The {@link IOException}
   */
  void tagAddKeywordsBatch(List<AddKeywordsRequest> addKeywordsRequests) throws IOException;

  /**
   * Delete keyword from a tag.
   *
   * @param deleteKeywordRequest contains info about the keyword to be deleted from a tag
   * @throws IOException The {@link IOException}
   */
  DeleteKeywordResponse tagDeleteKeyword(DeleteKeywordRequest deleteKeywordRequest) throws IOException;

  /**
   * Delete specific metadata from a tag.
   *
   * @param tagMetadataDeleteRequest contains info about the metadata to be deleted from a tag
   * @throws IOException The {@link IOException}
   */
  TagMetadataDeleteResponse tagMetadataDelete(TagMetadataDeleteRequest tagMetadataDeleteRequest) throws IOException;

  /**
   * Start activity types sync session.
   *
   * Initiates a sync session and responds with syncSessionId that can be used for
   * further activity types uploads within the session.
   *
   * While activity types can be sent to WiseTime in batches without a sync session,
   * starting a sync session for the batch uploads means that WiseTime will be able to detect activity types
   * that are no longer in the connected system, and delete these when the sync session is completed by the connector.
   *
   * @return created sync session
   * @throws IOException The {@link IOException}
   */
  SyncSession activityTypesStartSyncSession() throws IOException;

  /**
   * Complete activity types sync session.
   *
   * Completes a sync session so its syncSessionId can not be used anymore.
   * All the activity types that were lastly created/updated before the session start will be deleted.
   *
   * @param syncSession that should to be completed
   * @throws IOException The {@link IOException}
   */
  void activityTypesCompleteSyncSession(SyncSession syncSession) throws IOException;

  /**
   * Cancel activity types sync session
   *
   * Cancels a sync session so its syncSessionId can not be used anymore.
   * This API call has no impact on activity types.
   *
   * @param syncSession that should to be cancelled
   * @throws IOException The {@link IOException}
   */
  void activityTypesCancelSyncSession(SyncSession syncSession) throws IOException;

  /**
   * Create new activity types, or update existing in batch (up to 2000 items at once)
   *
   * @param syncActivityTypesRequest The {@link SyncActivityTypesRequest}
   * @throws IOException The {@link IOException}
   */
  SyncActivityTypesResponse syncActivityTypes(SyncActivityTypesRequest syncActivityTypesRequest) throws IOException;

  /**
   * Get the details for the team linked to the API key making the request.
   *
   * @return the team information
   * @throws IOException The {@link IOException}
   */
  TeamInfoResult teamInfo() throws IOException;

  /**
   * Subscribes a new webhook for receiving posted time. WiseTime will call your webhook whenever a user posts time to
   * your team.
   *
   * @param subscribeRequest information about the webhook to be created
   * @return the subscription result will contain the webhook ID that was assigned to the new webhook
   * @throws IOException The {@link IOException}
   */
  SubscribeResult postedTimeSubscribe(SubscribeRequest subscribeRequest) throws IOException;

  /**
   * Deletes a posted time webhook.
   *
   * @param unsubscribeRequest contains the webhook ID to be removed
   * @return {@link UnsubscribeResult} if webhook has been removed successfully
   * @throws IOException if request is unsuccessful
   */
  UnsubscribeResult postedTimeUnsubscribe(UnsubscribeRequest unsubscribeRequest) throws IOException;

  /**
   * Fetches a posted time group.
   *
   * @param limit the maximum amount of time groups received by call. Valid values: 1-25
   * @return a list of time groups. Or empty list if time out is reached.
   * @throws IOException if request is unsuccessful
   */
  List<TimeGroup> fetchTimeGroups(int limit) throws IOException;

  /**
   * Marks a time groups as either successfully posted or failed by id.
   *
   * @param timeGroupStatus contains the time group id, the actual status of the time group and an error message
   * @throws IOException if the request was unsuccessful
   */
  void updatePostedTimeStatus(TimeGroupStatus timeGroupStatus) throws IOException;

  /**
   * Retrieve configuration particulars to support the managed connector service.
   *
   * @param managedConfigRequest The {@link ManagedConfigRequest}
   * @return {@link ManagedConfigResponse} response result
   */
  ManagedConfigResponse getTeamManagedConfig(ManagedConfigRequest managedConfigRequest) throws IOException;

  /**
   * Report non-transient health check failure.
   * For unknown error codes the provided error message will be shown to users on time posting.
   * For known error codes connect-api-server will determine the appropriate message.
   *
   * @param request the error details
   * @throws IOException if the request was unsuccessful
   */
  void healthCheckFailureNotify(HealthCheckFailureNotify request) throws IOException;

  /**
   * Rescind (clear) a previously reported health check failure.
   *
   * @throws IOException if the request was unsuccessful
   */
  void healthCheckFailureRescind() throws IOException;
}
