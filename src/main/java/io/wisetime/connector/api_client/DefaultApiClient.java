/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.connector.logging.MessagePublisher;
import io.wisetime.connector.logging.WtEvent;
import io.wisetime.generated.connect.AddKeywordsRequest;
import io.wisetime.generated.connect.AddKeywordsResponse;
import io.wisetime.generated.connect.DeleteKeywordResponse;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.SubscribeRequest;
import io.wisetime.generated.connect.SubscribeResult;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

import static java.util.Optional.empty;

/**
 * Multi-thread implementation of {@link ApiClient}.
 * {@link RestRequestExecutor} is responsible for handling authentication.
 *
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public class DefaultApiClient implements ApiClient {

  private final RestRequestExecutor restRequestExecutor;
  private final ForkJoinPool forkJoinPool;
  private final MessagePublisher messagePublisher;

  public DefaultApiClient(RestRequestExecutor restRequestExecutor, MessagePublisher messagePublisher) {
    this.restRequestExecutor = restRequestExecutor;
    this.messagePublisher = messagePublisher;
    this.forkJoinPool = new ForkJoinPool(10);
  }

  @Override
  public void tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        UpsertTagResponse.class,
        EndpointPath.TagUpsert,
        upsertTagRequest
    );
    messagePublisher.publish(new WtEvent(WtEvent.Type.TAGS_UPSERTED, String.valueOf(1)));
  }

  /**
   * This implementation upserts the batch of tags in parallel.
   * It terminates early if an error is encountered while upserting tags.
   */
  @Override
  public void tagUpsertBatch(List<UpsertTagRequest> upsertTagRequests) throws IOException {

    final Callable<Optional<Exception>> parallelUntilError = () -> upsertTagRequests
        .parallelStream()
        // Wrap any exception with an Optional so we can short circuit the stream on error
        .map(request -> {
          try {
            tagUpsert(request);
            return Optional.<Exception>empty();
          } catch (Exception e) {
            return Optional.of(e);
          }
        })
        .filter(Optional::isPresent)
        .findFirst()
        .orElse(empty());

    Optional<Exception> error;

    try {
      // We use our own ForkJoinPool to have more control on the level of parallelism and
      // because we are IO bound. We don't want to affect other tasks on the default pool.
      error = forkJoinPool.submit(parallelUntilError).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
    if (!error.isPresent()) {
      messagePublisher.publish(new WtEvent(WtEvent.Type.TAGS_UPSERTED, String.valueOf(upsertTagRequests.size())));
    } else {
      throw new IOException("Failed to complete tag upsert batch. Stopped at error.", error.get());
    }
  }

  @Override
  public void tagDelete(String tagName) throws IOException {
    restRequestExecutor.executeTypedRequest(
        DeleteTagResponse.class,
        EndpointPath.TagDelete,
        Lists.newArrayList(new BasicNameValuePair("tagName", tagName))
    );
  }

  @Override
  public void tagAddKeywords(String tagName, Set<String> additionalKeywords) throws IOException {
    restRequestExecutor.executeTypedBodyRequest(
        AddKeywordsResponse.class,
        EndpointPath.TagAddKeyword,
        Lists.newArrayList(new BasicNameValuePair("tagName", tagName)),
        new AddKeywordsRequest().additionalKeywords(ImmutableList.copyOf(additionalKeywords))
    );
  }

  @Override
  public void tagAddKeywordsBatch(Map<String, Set<String>> tagNamesAndAdditionalKeywords) throws IOException {

    final Callable<Optional<Exception>> parallelUntilError = () -> tagNamesAndAdditionalKeywords
        .entrySet()
        .parallelStream()
        // Wrap any exception with an Optional so we can short circuit the stream on error
        .map(tagKeywords -> {
          try {
            tagAddKeywords(tagKeywords.getKey(), tagKeywords.getValue());
            return Optional.<Exception>empty();
          } catch (Exception e) {
            return Optional.of(e);
          }
        })
        .filter(Optional::isPresent)
        .findFirst()
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
  public void tagDeleteKeyword(String tagName, String keyword) throws IOException {
    restRequestExecutor.executeTypedRequest(
        DeleteKeywordResponse.class,
        EndpointPath.TagDeleteKeyword,
        Lists.newArrayList(
            new BasicNameValuePair("tagName", tagName),
            new BasicNameValuePair("keyword", keyword)
        )
    );
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
}
