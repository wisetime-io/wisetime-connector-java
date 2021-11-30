/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

public interface ResponseConsumer<T, R> {

  /**
   * Function is called for each response received.
   *
   * @param request the request sent to server
   * @return the response received from server
   */
  R consumeResponse(T request);

}
