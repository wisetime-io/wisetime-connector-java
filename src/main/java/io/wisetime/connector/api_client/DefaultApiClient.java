/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import com.google.common.collect.Lists;

import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.DeleteTagResponse;
import io.wisetime.generated.connect.TeamInfoResult;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

import static java.util.Optional.empty;

/**
 * @author thomas.haines@practiceinsight.io
 * @author shane.xie@practiceinsight.io
 */
public class DefaultApiClient implements ApiClient {

  private static final Logger log = LoggerFactory.getLogger(DefaultApiClient.class);
  private RestRequestExecutor restRequestExecutor;
  private ForkJoinPool forkJoinPool;

  public DefaultApiClient(RestRequestExecutor restRequestExecutor) {
    this.restRequestExecutor = restRequestExecutor;
    forkJoinPool = new ForkJoinPool(10);
  }

  @Override
  public UpsertTagResponse tagUpsert(UpsertTagRequest upsertTagRequest) throws IOException {
    return restRequestExecutor.executeTypedBodyRequest(
        UpsertTagResponse.class,
        EndpointPath.TagUpsert,
        upsertTagRequest
    );
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
      // It's nicer for client code to only have to deal with one type of exception
      throw new IOException(e);
    }
    if (error.isPresent()) {
      throw new IOException("Failed to complete tag upsert batch. Stopped at first error. ", error.get());
    }
  }

  @Override
  public DeleteTagResponse tagDelete(String tagName) throws IOException {
    return restRequestExecutor.executeTypedRequest(
        DeleteTagResponse.class,
        EndpointPath.TagDelete,
        Lists.newArrayList(new BasicHeader("tagName", tagName))
    );
  }

  public TeamInfoResult teamInfo() throws IOException {
    return restRequestExecutor.executeTypedRequest(TeamInfoResult.class, EndpointPath.TeamInfo);
  }
}
