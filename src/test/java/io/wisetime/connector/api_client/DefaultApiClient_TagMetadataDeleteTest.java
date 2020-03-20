package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.github.javafaker.Faker;
import io.wisetime.connector.api_client.support.RestRequestExecutor;
import io.wisetime.generated.connect.TagMetadataDeleteRequest;
import io.wisetime.generated.connect.TagMetadataDeleteResponse;
import java.io.IOException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dchandler
 */
public class DefaultApiClient_TagMetadataDeleteTest {

  private RestRequestExecutor requestExecutor;

  private DefaultApiClient apiClient;

  private Faker faker = Faker.instance();

  @BeforeEach
  void init() {
    requestExecutor = mock(RestRequestExecutor.class);
    apiClient = new DefaultApiClient(requestExecutor);
  }

  @Test
  void tagMetadataDelete_completes_on_no_error() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any(), any()))
        .thenReturn(new TagMetadataDeleteResponse());

    apiClient.tagMetadataDelete(fakeTagMetadataDeleteRequest());

    verify(requestExecutor, times(1)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagMetadataDelete.getClass()),
        any(TagMetadataDeleteRequest.class)
    );
  }

  @Test
  void tagMetadataDelete_wraps_exceptions() throws IOException {
    when(requestExecutor.executeTypedBodyRequest(any(), any(), any()))
        .thenThrow(new IOException());

    assertThatExceptionOfType(IOException.class).isThrownBy(() ->
        apiClient.tagMetadataDelete(fakeTagMetadataDeleteRequest())
    );
  }

  private TagMetadataDeleteRequest fakeTagMetadataDeleteRequest() {
    return new TagMetadataDeleteRequest()
        .tagName(faker.numerify("tagName###"))
        .metadataNames(Lists.newArrayList("meta_1", "meta_2", "meta_3"));
  }
}
