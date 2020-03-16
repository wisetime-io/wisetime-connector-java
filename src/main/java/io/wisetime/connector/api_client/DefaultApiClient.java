/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.core.type.TypeReference;

import io.wisetime.generated.connect.AddSetTagPropertiesRequest;
import io.wisetime.generated.connect.AddSetTagPropertiesResponse;
import io.wisetime.generated.connect.BatchUpsertTagRequest;
import io.wisetime.generated.connect.BatchUpsertTagResponse;
import io.wisetime.generated.connect.DeleteTagPropertiesRequest;
import io.wisetime.generated.connect.DeleteTagPropertiesResponse;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.AddKeywordsResponse;
import io.wisetime.generated.connect.DeleteKeywordRequest;
import io.wisetime.generated.connect.DeleteKeywordResponse;
import io.wisetime.generated.connect.DeleteTagRequest;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeGroupStatus;
import io.wisetime.generated.connect.UnsubscribeRequest;
import io.wisetime.generated.connect.UnsubscribeResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

import static java.util.Optional.empty;

/**
 * Multi-thread implementation of {@link ApiClient}. {@link RestRequestExecutor} is responsible for handling authentication.
 *
 * @author thomas.haines
 * @author shane.xie@practiceinsight.io
 */
public class DefaultApiClient implements ApiClient {

  private final RestRequestExecutor restRequestExecutor;
  private final ForkJoinPool forkJoinPool;

  public DefaultApiClient(String apiKey) {
    this(new RestRequestExecutor(apiKey));
  }

  DefaultApiClient(RestRequestExecutor requestExecutor) {
    this.restRequestExecutor = requestExecutor;
    forkJoinPool = new ForkJoinPool(10);
  }

  @Override
  public void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        UpsertTagResponse.class,
        EndpointPath.TagUpsert,
        upsertTagRequest
    );
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
    restRequestExecutor.executeTypedBodyRequest(
        DeleteTagResponse.class,
        EndpointPath.TagDelete,
        deleteTagRequest
    );
  }

  @Override
  public void tagAddKeywords(AddKeywordsRequest addKeywordsRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        AddKeywordsResponse.class,
        EndpointPath.TagAddKeyword,
        addKeywordsRequest
    );
  }

  @Override
  public void tagAddKeywordsBatch(List<AddKeywordsRequest> addKeywordsRequests) throws IOException {

    final Callable<Optional<Exception>> parallelUntilError = () -> addKeywordsRequests
        .parallelStream()
        // Wrap any exception with an Optional so we can short circuit the stream on error
        .map(tagKeywords -> {
          try {
            tagAddKeywords(tagKeywords);
            return Optional.<Exception>empty();
          } catch (Exception e) {
            return Optional.of(e);
          }
        })
        .filter(Optional::isPresent)
        .findAny()
        .orElse(empty());

    Optional<Exception> error;

    try {
      // We use our own ForkJoinPool to have more control on the level of parallelism and
      // because we are IO bound. We don't want to affect other tasks on the default pool.
      error = forkJoinPool.submit(parallelUntilError).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    if (error.isPresent()) {
      throw new IOException("Failed to complete tag keywords upsert batch. Stopped at error.", error.get());
    }
  }

  @Override
  public void tagDeleteKeyword(DeleteKeywordRequest deleteKeywordRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        DeleteKeywordResponse.class,
        EndpointPath.TagDeleteKeyword,
        deleteKeywordRequest
    );
  }

  @Override
  public void tagAddSetProperties(AddSetTagPropertiesRequest addSetTagPropertiesRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        AddSetTagPropertiesResponse.class,
        EndpointPath.TagAddSetProperties,
        addSetTagPropertiesRequest
    );
  }

  @Override
  public void tagDeleteProperties(DeleteTagPropertiesRequest deleteTagPropertiesRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        DeleteTagPropertiesResponse.class,
        EndpointPath.TagDeleteProperties,
        deleteTagPropertiesRequest
    );
  }

  @Override
  public void tagAddSetPropertiesBatch(List<AddSetTagPropertiesRequest> addSetTagPropertiesRequests)
      throws IOException {

    final Callable<Optional<Exception>> parallelUntilError = () -> addSetTagPropertiesRequests
        .parallelStream()
        // Wrap any exception with an Optional so we can short circuit the stream on error
        .map(tagProperties -> {
          try {
            tagAddSetProperties(tagProperties);
            return Optional.<Exception>empty();
          } catch (Exception e) {
            return Optional.of(e);
          }
        })
        .filter(Optional::isPresent)
        .findAny()
        .orElse(empty());

    Optional<Exception> error;

    try {
      // We use our own ForkJoinPool to have more control on the level of parallelism and
      // because we are IO bound. We don't want to affect other tasks on the default pool.
      error = forkJoinPool.submit(parallelUntilError).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    if (error.isPresent()) {
      throw new IOException("Failed to complete tag properties upsert batch. Stopped at error.", error.get());
    }
  }

  public TeamInfoResult teamInfo() throws IOException {
    return restRequestExecutor.executeTypedRequest(TeamInfoResult.class, EndpointPath.TeamInfo);
  }

  @Override
  public SubscribeResult postedTimeSubscribe(SubscribeRequest subscribeRequest) throws IOException {
    return restRequestExecutor.executeTypedBodyRequest(
        SubscribeResult.class,
        EndpointPath.PostedTimeSubscribe,
        subscribeRequest
    );
  }

  @Override
  public UnsubscribeResult postedTimeUnsubscribe(UnsubscribeRequest unsubscribeRequest) throws IOException {
    return restRequestExecutor.executeTypedBodyRequest(
        UnsubscribeResult.class,
        EndpointPath.PostedTimeUnsubscribe,
        unsubscribeRequest
    );
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
}
