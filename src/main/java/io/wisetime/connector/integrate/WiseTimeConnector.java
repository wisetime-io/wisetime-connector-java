/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.integrate;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.generated.connect.TimeGroup;
import spark.Request;

/**
 * Main extension point of application. User will have to implement this interface and provide it during building
 * {@link io.wisetime.connector.ConnectorRunner}.
 *
 * @author thomas.haines@practiceinsight.io
 */
public interface WiseTimeConnector {

  /**
   * Called once after server initialisation.  The init method will be called before TagUpdate or PostTime methods are
   * called.
   * @param connectorModule dependencies are passed to the connector via this module
   */
  void init(ConnectorModule connectorModule);

  /**
   * Called on a schedule. If the previously called method is still running when the next scheduled run should occur, the
   * scheduled run will be skipped, allowing time for the previous method to complete it's operation.
   */
  void performTagUpdate();

  /**
   * Called via the defined webhook or via the listening fetch client when a user posts time to the given team.
   * <p>
   * If a RunTimeException is thrown, this will be treated as a transient error, the operation will be retried after a
   * delay.
   *
   * @param request        The full request that the server received or null if the fetch client is used.
   * @param userPostedTime For convenience, the json body of the request is provided as a Java object model representation.
   * @return The result of the post operation.
   *

   */
  PostResult postTime(Request request, TimeGroup userPostedTime);

  /**
   * @return Whether the connector is in a healthy state; as one example, can a critical service such as a database or
   * endpoint be reached at present?
   */
  default boolean isConnectorHealthy() {
    return true;
  }

  /**
   * Called when application is about to stop. Implementations should free the resources that the connector have acquired.
   */
  default void shutdown() {}
}
