/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import org.apache.http.client.HttpResponseException;

/**
 * Exception class that additionally to {@link HttpResponseException} holds response content.
 *
 * @author bartlomiej.mocior@gmail.com
 */
public class HttpClientResponseException extends HttpResponseException {

  private final String content;
  private final int statusCode;
  private final String message;

  public HttpClientResponseException(int statusCode, String message, String content) {
    super(statusCode, message);
    this.content = content;
    this.statusCode = statusCode;
    this.message = message;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "HttpClientResponseException{" +
        "content='" + content + '\'' +
        ", statusCode=" + statusCode +
        ", message='" + message + '\'' +
        '}';
  }
}
