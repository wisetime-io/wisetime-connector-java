/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.wisetime.connector.api_client.ApiClient;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.connector.test_util.SparkTestUtil;
import io.wisetime.connector.test_util.TemporaryFolder;
import io.wisetime.connector.test_util.TemporaryFolderExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author thomas.haines@practiceinsight.io
 */
@ExtendWith(TemporaryFolderExtension.class)
@SuppressWarnings("WeakerAccess")
public class ServerStartTest {
  private static final Logger log = LoggerFactory.getLogger(ServerStartTest.class);

  static TemporaryFolder testExtension;

  @Test
  void startAndQuery() throws Exception {
    log.info(testExtension.newFolder().getAbsolutePath());
    Server server = createTestServer();

    SparkTestUtil testUtil = new SparkTestUtil(getPort(server));
    SparkTestUtil.UrlResponse response = testUtil.doMethod("GET", "/ping", null, "plain/text");
    assertThat(response.status).isEqualTo(200);

    assertThat(response.body)
        .as("expect pong response")
        .isEqualTo("pong");

    if (System.getProperty("examine") != null) {
      server.join();
    } else {
      server.stop();
    }
  }

  public static Server createTestServer() throws Exception {
    WiseTimeConnector mockConnector = mock(WiseTimeConnector.class);
    ApiClient mockApiClient = mock(ApiClient.class);
    ServerRunner runner = ServerRunner.createServerBuilder()
        .withWiseTimeConnector(mockConnector)
        .withApiClient(mockApiClient)
        .withPort(0)
        .useSlf4JOnly(true)
        .build();
    long startTime = System.currentTimeMillis();

    runner.startServer(false);
    Server server = runner.getServer();
    SparkTestUtil testUtil = new SparkTestUtil(getPort(server));

    RetryPolicy retryPolicy = new RetryPolicy()
        .retryOn(Exception.class)
        .withDelay(100, TimeUnit.MILLISECONDS)
        .withMaxRetries(40);

    Failsafe.with(retryPolicy).run(() -> getHome(testUtil));

    log.info((System.currentTimeMillis() - startTime) + "ms server start http://localhost:{}", getPort(server));
    return server;
  }

  private static void getHome(SparkTestUtil testUtil) throws Exception {
    SparkTestUtil.UrlResponse response = testUtil.doMethod("GET", "/", null, "text/html");
    assertThat(response.status).isEqualTo(200);
  }

  public static int getPort(Server server) {
    ServerConnector connector = (ServerConnector) server.getConnectors()[0];
    return connector.getLocalPort() <= 0 ? connector.getPort() : connector.getLocalPort();
  }
}
