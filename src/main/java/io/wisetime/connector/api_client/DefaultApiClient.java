/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import io.wisetime.connector.api_client.AddKeywordsResult.AddKeywordsStatus;
import io.wisetime.connector.api_client.support.HttpClientResponseException;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.utils.EmptyResponse;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.BatchUpsertTagRequest;
import io.wisetime.generated.connect.BatchUpsertTagResponse;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import io.wisetime.generated.connect.SyncActivityTypesRequest;
import io.wisetime.generated.connect.SyncActivityTypesResponse;
import io.wisetime.generated.connect.SyncSession;
import io.wisetime.generated.connect.TagMetadataDeleteRequest;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UpsertTagRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import org.apache.http.message.BasicNameValuePair;

/**
 * Multi-thread implementation of {@link ApiClient}. {@link RestRequestExecutor} is responsible for handling authentication.
 *
 * @author thomas.haines
 * @author shane.xie@practiceinsight.io
 */
public class DefaultApiClient implements ApiClient {

  private final RestRequestExecutor restRequestExecutor;
  private final ExecutorService executorService;

  public DefaultApiClient(String apiKey) {
    this(new RestRequestExecutor(apiKey));
  }

  public DefaultApiClient(RestRequestExecutor requestExecutor) {
    this.restRequestExecutor = requestExecutor;
    executorService = Executors.newFixedThreadPool(6, new ThreadFactory() {
      final ThreadFactory threadFactory = Executors.defaultThreadFactory();

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = threadFactory.newThread(r);
        thread.setName("tagAddKeywords-" + thread.getName());
        return thread;
      }
    });
    Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
  }

  @Override
  public void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.TagUpsert, upsertTagRequest);
  }

  @Override
  public void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException {
    BatchUpsertTagResponse response = restRequestExecutor.executeTypedBodyRequest(
        BatchUpsertTagResponse.class,
        EndpointPath.BulkTagUpsert,
        new BatchUpsertTagRequest().tags(upsertTagRequests)
    );
    if (!response.getErrors().isEmpty()) {
      throw new IOException("Received errors while upserting tags: " + response.getErrors());
    }
  }

  @Override
  public void tagDelete(DeleteTagRequest deleteTagRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.TagDelete, deleteTagRequest);
  }

  @Override
  public AddKeywordsResult tagAddKeywords(AddKeywordsRequest addKeywordsRequest) throws IOException {
    try {
      restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.TagAddKeyword, addKeywordsRequest);
      return new AddKeywordsResult(addKeywordsRequest.getTagName(), AddKeywordsStatus.SUCCESS);
    } catch (HttpClientResponseException e) {
      if (e.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
        return new AddKeywordsResult(addKeywordsRequest.getTagName(), AddKeywordsStatus.TAG_NOT_FOUND);
      }
      throw e;
    }
  }

  @Override
  public List<AddKeywordsResult> tagAddKeywordsBatch(List<AddKeywordsRequest> addKeywordsRequests) throws IOException {
    List<Future<AddKeywordsResult>> futures = addKeywordsRequests.stream()
        .map(request -> executorService.submit(() -> {
          try {
            return tagAddKeywords(request);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })).collect(Collectors.toList());
    try {
      List<AddKeywordsResult> result = new ArrayList<>(addKeywordsRequests.size());
      for (Future<AddKeywordsResult> future : futures) {
        result.add(future.get());
      }
      return result;
    } catch (InterruptedException | ExecutionException e) {
      futures.forEach(future -> future.cancel(false));
      throw new IOException("Failed to execute tagAddKeywordsBatch", e.getCause());
    }
  }

  @Override
  public void tagDeleteKeyword(DeleteKeywordRequest deleteKeywordRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.TagDeleteKeyword, deleteKeywordRequest);
  }

  @Override
  public void tagMetadataDelete(TagMetadataDeleteRequest tagMetadataDeleteRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.TagMetadataDelete, tagMetadataDeleteRequest);
  }

  @Override
  public SyncSession activityTypesStartSyncSession() throws IOException {
    return restRequestExecutor.executeTypedRequest(SyncSession.class, EndpointPath.ActivityTypesStartSyncSession);
  }

  @Override
  public void activityTypesCompleteSyncSession(SyncSession syncSession) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.ActivityTypesCompleteSyncSession, syncSession);
  }

  @Override
  public void activityTypesCancelSyncSession(SyncSession syncSession) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.ActivityTypesCancelSyncSession, syncSession);
  }

  @Override
  public SyncActivityTypesResponse syncActivityTypes(SyncActivityTypesRequest syncActivityTypesRequest) throws IOException {
    return restRequestExecutor.executeTypedBodyRequest(
        SyncActivityTypesResponse.class,
        EndpointPath.BatchActivityTypesUpsert,
        syncActivityTypesRequest
    );
  }

  public TeamInfoResult teamInfo() throws IOException {
    return restRequestExecutor.executeTypedRequest(TeamInfoResult.class, EndpointPath.TeamInfo);
  }

  @Override
  public List<TimeGroup> fetchTimeGroups(int limit) throws IOException {
    return restRequestExecutor.executeTypedRequest(new TypeReference<List<TimeGroup>>(){},
        EndpointPath.PostedTimeFetch,
        ImmutableList.of(new BasicNameValuePair("limit", String.valueOf(limit))));
  }

  @Override
  public void updatePostedTimeStatus(TimeGroupStatus timeGroupStatus) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(Object.class, EndpointPath.PostedTimeUpdateStatus, timeGroupStatus);
  }

  @Override
  public ManagedConfigResponse getTeamManagedConfig(ManagedConfigRequest managedConfigRequest) throws IOException {
    return restRequestExecutor.executeTypedBodyRequest(
        ManagedConfigResponse.class,
        EndpointPath.TeamManagedConfig,
        managedConfigRequest
    );
  }

  @Override
  public void healthCheckFailureNotify(HealthCheckFailureNotify request) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(EmptyResponse.class, EndpointPath.HealthCheckFailureNotify, request);
  }

  @Override
  public void healthCheckFailureRescind() throws IOException {
    restRequestExecutor.executeRequest(EndpointPath.HealthCheckFailureRescind, ImmutableList.of());
  }
}
