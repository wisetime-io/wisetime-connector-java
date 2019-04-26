package io.wisetime.connector.fetch_client;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.integrate.WiseTimeConnector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author thomas.haines
 */
@RequiredArgsConstructor
@Getter
public class FetchClientSpec {
  private final ApiClient apiClient;
  private final WiseTimeConnector connector;
  private final TimeGroupIdStore timeGroupIdStore;
  private final int limit;
}
