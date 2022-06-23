/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.generated.connect.HealthCheckFailureNotify;
import io.wisetime.generated.connect.HealthCheckFailureNotify.ErrorTypeEnum;
import io.wisetime.generated.connect.TimeGroup;
import java.util.Optional;

/**
 * Main extension point of application. User will have to implement this interface and provide it during building
 * with {@link ConnectorController#newBuilder()}.
 *
 * @author thomas.haines
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
   * Called on a schedule. If the previously called method is still running when the next scheduled run should occur, the
   * scheduled run will be skipped, allowing time for the previous method to complete it's operation.
   * This will be called less frequently than performTagUpdate and allows to refresh already synced tags slowly.
   */
  void performTagUpdateSlowLoop();

  /**
   * Called on a schedule. If the previously called method is still running when the next scheduled run should occur, the
   * scheduled run will be skipped, allowing time for the previous method to complete it's operation.
   */
  default void performActivityTypeUpdate() {
    // default no activity type update supplied
  }

  /**
   * Called on a schedule. If the previously called method is still running when the next scheduled run should occur, the
   * scheduled run will be skipped, allowing time for the previous method to complete it's operation.
   * This will be called less frequently than performActivityTypeUpdate and allows to refresh already synced activity types
   * slowly.
   */
  default void performActivityTypeUpdateSlowLoop() {
    // default no activity type update slow loop supplied
  }

  /**
   * Called via the defined webhook or via the listening fetch client when a user posts time to the given team.
   * <p>
   * If a RunTimeException is thrown, this will be treated as a transient error, the operation will be retried after a
   * delay.
   *
   * @param userPostedTime For convenience, the json body of the request is provided as a Java object model representation.
   * @return The result of the post operation.
   *
   */
  PostResult postTime(TimeGroup userPostedTime);

  /**
   * Identifies the type of the connector.  A non-empty string value would be assigned by the implementation.
   *
   * @return the type of connector
   */
  default String getConnectorType() {
    return "";
  }

  /**
   * use {@link #checkHealth()} instead to report reach errors
   *
   * @return Whether the connector is in a healthy state; as one example, can a critical service such as a database or
   * endpoint be reached at present? null if no failures
   */
  @Deprecated
  default boolean isConnectorHealthy() {
    return true;
  }

  /**
   * Check if connector is health or not.
   */
  default Optional<HealthCheckFailureNotify> checkHealth() {
    return isConnectorHealthy()
        ? Optional.empty() : Optional.of(new HealthCheckFailureNotify().errorType(ErrorTypeEnum.UNKNOWN));
  }

  /**
   * Called when application is about to stop. Implementations should free the resources that the connector have acquired.
   */
  default void shutdown() {

  }
}
