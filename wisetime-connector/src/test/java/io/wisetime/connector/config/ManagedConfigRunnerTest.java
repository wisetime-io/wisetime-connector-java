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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author dchandler
 */
class ManagedConfigRunnerTest {

  private final EnhancedRandom random = aNewEnhancedRandom();

  private WiseTimeConnector connectorMock;
  private ApiClient apiClientMock;
  private ConnectorInfoProvider connectorInfoProviderMock;
  private ManagedConfigRunner managedConfigRunner;

  @BeforeEach
  void setup() throws Exception {
    connectorMock = mock(WiseTimeConnector.class);
    apiClientMock = mock(ApiClient.class);
    connectorInfoProviderMock = mock(ConnectorInfoProvider.class);

    when(connectorInfoProviderMock.get()).thenReturn(new ConnectorInfo("+08:00"));
    when(connectorMock.getConnectorType()).thenReturn("test-connector-type");
    when(apiClientMock.getTeamManagedConfig(any())).thenReturn(random.nextObject(ManagedConfigResponse.class));

    managedConfigRunner = spy(new ManagedConfigRunner(connectorMock, apiClientMock, connectorInfoProviderMock));
  }

  @Test
  void testRun() throws Exception {
    managedConfigRunner.setCachedServiceExpiryDate(ZonedDateTime.now());

    ZonedDateTime startRun = managedConfigRunner.getLastSuccessfulRun();
    ZonedDateTime expiryDate = managedConfigRunner.getCachedServiceExpiryDate();

    Thread.sleep(1);

    doAnswer(invocation -> {
      Object configResponse = invocation.getArgument(0);
      assertThat(configResponse).isNotNull();

      return expiryDate.plusHours(12);

    }).when(managedConfigRunner).onManagedConfigResponse(any(), any());

    managedConfigRunner.run();

    assertThat(managedConfigRunner.getCachedServiceExpiryDate().toLocalDateTime().isAfter(expiryDate.toLocalDateTime()))
        .as("expect expiry date was retrieved")
        .isTrue();

    assertThat(managedConfigRunner.getLastSuccessfulRun().toInstant().toEpochMilli())
        .as("expect last success was updated")
        .isGreaterThan(startRun.toInstant().toEpochMilli());

    verify(apiClientMock, times(1)).getTeamManagedConfig(any());
    verify(connectorMock, times(1)).getConnectorType();
    verify(connectorInfoProviderMock, times(2)).get();
  }

  @Test
  void testIsHealthy() {
    assertThat(managedConfigRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    managedConfigRunner.setLastSuccessfulRun(ZonedDateTime.now().minusYears(1));
    assertThat(managedConfigRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }

  @Test
  void createManageConfigRequest() {
    final ConnectorInfo connectorInfo = connectorInfoProviderMock.get();

    final ManagedConfigRequest configRequest = managedConfigRunner.createManageConfigRequest();
    assertThat(configRequest)
        .extracting(ManagedConfigRequest::getConnectorType,
            ManagedConfigRequest::getClientTimeZoneOffset)
        .containsExactly(connectorMock.getConnectorType(),
            connectorInfo.getClientTimeZoneOffset());

    assertThat(configRequest.getEnvironment()).isNotEmpty();
  }

  @Test
  void testToExpiryZoneDateTime() {
    final LocalDateTime serviceExpiryDate = LocalDateTime.now(ZoneId.of("+02:00"));
    ZonedDateTime zonedDateTime = serviceExpiryDate.atZone(ZoneId.of("+02:00"));
    final ZonedDateTime zonedServiceExpiryDate = managedConfigRunner.toExpiryZoneDateTime(
        zonedDateTime.toOffsetDateTime(), ZoneId.of("+02:00"));
    assertThat(zonedServiceExpiryDate).isNotNull();
    assertThat(zonedServiceExpiryDate)
        .isEqualToIgnoringSeconds(ZonedDateTime.now(ZoneId.of("+02:00")));
  }
}
