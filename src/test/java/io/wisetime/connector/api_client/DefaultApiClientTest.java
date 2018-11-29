/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.UpsertTagRequest;
import io.wisetime.generated.connect.UpsertTagResponse;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author shane.xie@practiceinsight.io
 */
public class DefaultApiClientTest {

  private RestRequestExecutor requestExecutor;
  private DefaultApiClient apiClient;

  @BeforeEach
  void init() {
    requestExecutor = mock(RestRequestExecutor.class);
    apiClient = new DefaultApiClient(requestExecutor);
  }

  @Test
  void tagUpsertBatch_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new UpsertTagResponse());

    apiClient.tagUpsertBatch(fakeUpsertTagRequests(5));

    verify(requestExecutor, times(5)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagUpsert.getClass()),
        any(UpsertTagRequest.class)
    );
  }

  @Test
  void tagUpsertBatch_stops_on_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new UpsertTagResponse())
        .thenThrow(new IOException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagUpsertBatch(fakeUpsertTagRequests(1000))
    );

    // At least 10 requests made in parallel before we notice an error.
    verify(requestExecutor, atLeast(10)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagUpsert.getClass()),
        any(UpsertTagRequest.class)
    );

    // We should notice that a request has failed way before we reach the end of the list
    verify(requestExecutor, atMost(20)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagUpsert.getClass()),
        any(UpsertTagRequest.class)
    );
  }

  @Test
  void tagUpsertBatch_wraps_exceptions() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenThrow(new RuntimeException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagUpsertBatch(fakeUpsertTagRequests(3))
    );
  }

  private List<UpsertTagRequest> fakeUpsertTagRequests(final int numberOfRequests) {
    final ArrayList<UpsertTagRequest> requests = new ArrayList<>();
    IntStream
        .range(1, numberOfRequests + 1)
        .forEach(i -> requests.add(new UpsertTagRequest().name(String.valueOf(i))));
    return requests;
  }
}
