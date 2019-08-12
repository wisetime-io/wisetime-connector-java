/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static io.wisetime.connector.log.LogbackConfigurator.SERVICE_ID_DELIMITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.javafaker.Faker;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.LifeCycle;
import io.wisetime.connector.config.RuntimeConfig;

/**
 * @author thomas.haines
 */
class LogbackConfiguratorTest {

  private static Faker FAKER = Faker.instance();

  @Test
  void setLogLevel() {
    // if not configured, no action/exception
    LogbackConfigurator.setLogLevel();
  }

  @Test
  void addLocalAdapter() {
    final ManagedConfigResponse configMock = mock(ManagedConfigResponse.class);

    // in env without config or explicit disable, should fail quietly
    RuntimeConfig.setProperty(() -> "DISABLE_AWS_CRED_USAGE", "true");
    Optional<Appender<ILoggingEvent>> localAdapter = LogbackConfigurator.createLocalAdapter(configMock);
    localAdapter.ifPresent(LifeCycle::stop);
  }

  @Test
  void getServiceCredentials() {
    final String accessKeyId = FAKER.numerify("awsId####");
    final String secretKey = FAKER.numerify("SecretKey####");

    final String serviceIdBase64 = Base64.getUrlEncoder().encodeToString(
        String.format("%s%s%s", accessKeyId, SERVICE_ID_DELIMITER, secretKey).getBytes());

    final Pair<String, String> creds = LogbackConfigurator.getServiceCredentials(serviceIdBase64);
    assertThat(creds.getLeft()).isEqualTo(accessKeyId);
    assertThat(creds.getRight()).isEqualTo(secretKey);
  }
}