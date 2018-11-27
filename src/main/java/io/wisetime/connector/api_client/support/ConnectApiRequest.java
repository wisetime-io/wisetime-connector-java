/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.annotation.Nullable;

/**
 * Encapsulate a conventional http request and add authentication authentication headers
 *
 * @author pascal
 */
public class ConnectApiRequest {

  private static final Logger log = LoggerFactory.getLogger(ConnectApiRequest.class);
  private final HttpMethod method;
  private final String uri;
  private final Authorizer authorizer;
  private final String jsonBody;

  /**
   * @param jsonBody If json body is to be set for endpoint request, include the json String in constructor.
   */
  ConnectApiRequest(final HttpMethod method,
                    final String uri,
                    Authorizer authorizer,
                    @Nullable String jsonBody) {
    // request is created in execute
    this.method = method;
    this.uri = uri;
    this.authorizer = authorizer;
    this.jsonBody = jsonBody;
  }

  String execute() throws IOException {
    Request request = createRequest();
    if (jsonBody != null) {
      request.bodyString(jsonBody, ContentType.APPLICATION_JSON);
    }
    log.debug("request body: " + request.toString());
    final String content;
    HttpClient httpClient = HttpClientProvider.getHttpClient();
    Executor executor = Executor.newInstance(httpClient);
    Response response = executor.execute(request);

    final HttpResponse httpResponse = response.returnResponse();
    final StatusLine statusLine = httpResponse.getStatusLine();
    final HttpEntity entity = httpResponse.getEntity();
    if (entity != null) {
      content = EntityUtils.toString(entity);
    } else {
      content = null;
    }

    log.debug("response ({}): {}", statusLine.getStatusCode(), content);

    if (statusLine.getStatusCode() >= 300) {
      final HttpClientResponseException responseException = new HttpClientResponseException(
          statusLine.getStatusCode(), statusLine.getReasonPhrase(), content
      );
      log.warn("server returned error: {}", responseException.toString());

      throw responseException;
    }

    return content;
  }

  private Request createRequest() {
    Request request;
    switch (method) {
      case POST:
        request = Request.Post(uri);
        break;
      case PATCH:
        request = Request.Patch(uri);
        break;
      case DELETE:
        request = Request.Delete(uri);
        break;
      case PUT:
        request = Request.Put(uri);
        break;
      default:
      case GET:
        request = Request.Get(uri);
    }

    authorizer.authorize(request);
    return request;
  }

  /**
   * Enum of http methods
   */
  public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
  }

  /**
   * Apply authorization parameter(s) to the request (as required).
   */
  public interface Authorizer {
    void authorize(Request request);
  }
}
