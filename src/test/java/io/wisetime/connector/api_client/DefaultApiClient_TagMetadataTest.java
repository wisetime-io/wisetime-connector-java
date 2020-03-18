package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.TagMetadataUpdateRequest;
import io.wisetime.generated.connect.TagMetadataUpdateResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dchandler
 */
public class DefaultApiClient_TagMetadataTest {

  private RestRequestExecutor requestExecutor;

  private DefaultApiClient apiClient;

  @BeforeEach
  void init() {
    requestExecutor = mock(RestRequestExecutor.class);
    apiClient = new DefaultApiClient(requestExecutor);
  }

  @Test
  void tagMetadataUpdateBatch_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(), any()))
        .thenReturn(new TagMetadataUpdateResponse());

    apiClient.tagMetadataUpdateBatch(fakeTagMetadataUpdateRequestList(5));

    verify(requestExecutor, times(5)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagMetadataUpdate.getClass()),
        any(TagMetadataUpdateRequest.class)
    );
  }

  @Test
  void tagMetadataUpdateBatch_stops_on_error() throws IOException {
    // mockito answer is not synchronised. it is not guaranteed that only 1 return will be TagMetadataUpdateResponse
    // on thread race
    IOException expectedException = new IOException();
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenReturn(new TagMetadataUpdateResponse())
        .thenThrow(expectedException);

    assertThatThrownBy(() -> apiClient.tagMetadataUpdateBatch(fakeTagMetadataUpdateRequestList(1000)))
        .as("we expecting first requests pass and than expected exception to be thrown")
        .hasMessage("Failed to complete tag metadata update batch. Stopped at error.")
        .hasCause(expectedException);

    // We should notice that a request has failed way before we reach the end of the list
    // Allowance is made for requests sent in parallel before we notice an error
    // number of requests should always be less than 2*pool_size
    verify(requestExecutor, atMost(20)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagMetadataUpdate.getClass()),
        any(TagMetadataUpdateRequest.class)
    );
  }

  @Test
  void tagMetadataUpdateBatch_wraps_exceptions() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenThrow(new RuntimeException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagMetadataUpdateBatch(fakeTagMetadataUpdateRequestList(3))
    );
  }

  private List<TagMetadataUpdateRequest> fakeTagMetadataUpdateRequestList(final int numberOfTags) {
    final List<TagMetadataUpdateRequest> requests = new ArrayList<>();
    IntStream
        .range(1, numberOfTags + 1)
        .forEach(i -> {
          Map<String, String> metadataMap = Collections.unmodifiableMap(new HashMap<String, String>() {
            {
              put(String.valueOf(i), String.valueOf(i));
            }
          });
          requests.add(new TagMetadataUpdateRequest()
              .tagName(String.valueOf(i))
              .metadata(metadataMap));
        });
    return requests;
  }
}
