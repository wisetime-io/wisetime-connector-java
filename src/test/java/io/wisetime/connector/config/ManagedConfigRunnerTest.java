/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import static io.github.benas.randombeans.EnhancedRandomBuilder.aNewEnhancedRandom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.config.info.ConnectorInfo;
import io.wisetime.connector.config.info.ConnectorInfoProvider;
import io.wisetime.generated.connect.ManagedConfigRequest;
import io.wisetime.generated.connect.ManagedConfigResponse;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.groups.Tuple;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dchandler
 */
class ManagedConfigRunnerTest {

  private WiseTimeConnector connectorMock;

  private ApiClient apiClientMock;

  private ConnectorInfoProvider connectorInfoProviderMock;

  private ManagedConfigRunner managedConfigRunner;

  private EnhancedRandom random = aNewEnhancedRandom();

  @BeforeEach
  void setup() throws Exception {
    connectorMock = mock(WiseTimeConnector.class);
    apiClientMock = mock(ApiClient.class);
    connectorInfoProviderMock = mock(ConnectorInfoProvider.class);

    when(connectorInfoProviderMock.get()).thenReturn(random.nextObject(ConnectorInfo.class));
    when(connectorMock.getConnectorType()).thenReturn("test-connector-type");
    when(apiClientMock.getTeamManagedConfig(any())).thenReturn(random.nextObject(ManagedConfigResponse.class));

    managedConfigRunner = spy(new ManagedConfigRunner(connectorMock, apiClientMock, connectorInfoProviderMock));
  }

  @Test
  void testRun() throws Exception {
    managedConfigRunner.cachedServiceExpiryDate = DateTime.now();

    DateTime startRun = managedConfigRunner.lastSuccessfulRun;
    DateTime expiryDate = managedConfigRunner.cachedServiceExpiryDate;

    Thread.sleep(1);

    doAnswer(invocation -> {
      Object configResponse = invocation.getArgument(0);
      assertThat(configResponse).isNotNull();

      return expiryDate.plusHours(12);

    }).when(managedConfigRunner).onManagedConfigResponse(any());

    managedConfigRunner.run();

    assertThat(managedConfigRunner.cachedServiceExpiryDate)
        .as("expect expiry date was retrieved")
        .isGreaterThan(expiryDate);

    assertThat(managedConfigRunner.lastSuccessfulRun)
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(apiClientMock, times(1)).getTeamManagedConfig(any());
    verify(connectorMock, times(1)).getConnectorType();
    verify(connectorInfoProviderMock, times(1)).get();
  }

  @Test
  void testIsHealthy() {
    assertThat(managedConfigRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    managedConfigRunner.lastSuccessfulRun = new DateTime().minusYears(1);
    assertThat(managedConfigRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }

  @Test
  void createManageConfigRequest() {
    final ConnectorInfo connectorInfo = connectorInfoProviderMock.get();

    ManagedConfigRequest configRequest = managedConfigRunner.createManageConfigRequest();
    assertThat(configRequest)
        .extracting(ManagedConfigRequest::getConnectorType,
            ManagedConfigRequest::getClientTimeZoneOffset)
        .containsExactly(connectorMock.getConnectorType(),
            connectorInfo.getClientTimeZoneOffset());

    assertThat(configRequest.getEnvironment()).isNotEmpty();
  }
}
