package io.wisetime.connector.api_client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;

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
    final TagMetadataDeleteResponse response = new TagMetadataDeleteResponse();
    final TagMetadataDeleteRequest request = fakeTagMetadataDeleteRequest();

    when(requestExecutor.executeTypedBodyRequest(any(), any(), eq(request)))
        .thenReturn(response);

    TagMetadataDeleteResponse result = apiClient.tagMetadataDelete(request);
    assertThat(result)
        .as("get response is passed back")
        .isEqualTo(response);

    ArgumentCaptor<TagMetadataDeleteRequest> captor = ArgumentCaptor.forClass(TagMetadataDeleteRequest.class);
    verify(requestExecutor, times(1)).executeTypedBodyRequest(
        any(),
        any(EndpointPath.TagMetadataDelete.getClass()),
        captor.capture()
    );

    assertThat(captor.getValue())
        .as("pass input to serialiser")
        .isEqualTo(request);
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
