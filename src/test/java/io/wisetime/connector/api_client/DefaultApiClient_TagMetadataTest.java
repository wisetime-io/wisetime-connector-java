package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.github.javafaker.Faker;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.TagMetadataUpdateRequest;
import io.wisetime.generated.connect.TagMetadataUpdateResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dchandler
 */
public class DefaultApiClient_TagMetadataTest {

  private RestRequestExecutor requestExecutor;

  private DefaultApiClient apiClient;

  private Faker faker = Faker.instance();

  @BeforeEach
  void init() {
    requestExecutor = mock(RestRequestExecutor.class);
    apiClient = new DefaultApiClient(requestExecutor);
  }

  @Test
  void tagMetadataUpdate_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(), any()))
        .thenReturn(new TagMetadataUpdateResponse());

    apiClient.tagMetadataUpdate(fakeTagMetadataUpdateRequest());

    verify(requestExecutor, times(1)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagMetadataUpdate.getClass()),
        any(TagMetadataUpdateRequest.class)
    );
  }

  @Test
  void tagMetadataUpdateBatch_wraps_exceptions() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenThrow(new IOException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagMetadataUpdate(fakeTagMetadataUpdateRequest())
    );
  }

  private TagMetadataUpdateRequest fakeTagMetadataUpdateRequest() {
    Map<String, String> metadataMap = Collections.unmodifiableMap(new HashMap<String, String>() {
      {
        put(faker.numerify("meta-key###"), faker.numerify("meta-value###"));
      }
    });
    return new TagMetadataUpdateRequest()
        .tagName(faker.numerify("tagName###"))
        .metadata(metadataMap);
  }
}
