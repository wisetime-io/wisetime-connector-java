/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.wisetime.connector.test_util.SparkTestUtil;
import io.wisetime.connector.test_util.SparkTestUtil.UrlResponse;
import io.wisetime.connector.time_poster.webhook.SparkWebApp;
import spark.ExceptionHandler;
import spark.Response;
import spark.servlet.SparkApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static spark.Spark.get;

/**
 * Because of the sharable spies, this test isn't thread-safe. In case of enabling JUnit parallel methods invocation, all
 * tests should be moved to a single method.
 *
 * @author yehor.lashkul
 */
class SparkUtilTest {

  private static final String SUCCESS_PATH = "/success";
  private static final String SUCCESS_MSG = "Success";

  private static final String ILLEGAL_ARG_ERROR_PATH = "/illegalArgument";
  private static final String ILLEGAL_ARG_ERROR_THROWN = "Illegal argument";
  private static final String ILLEGAL_ARG_ERROR_HANDLED = "Illegal argument handled";

  private static final String UNEXPECTED_ERROR_PATH = "/unexpected";
  private static final String UNEXPECTED_ERROR_THROWN = "Unexpected error";
  private static final String UNEXPECTED_ERROR_HANDLED = "Unexpected error handled";

  private static final String NOT_HANDLED_ERROR_PATH = "/notHandled";
  private static final String NOT_HANDLED_ERROR_THROWN = "Not Handled error";

  private static ExceptionHandler<IllegalArgumentException> illegalArgExceptionHandler;
  private static ExceptionHandler<Exception> unexpectedExceptionHandler;
  private static SparkTestUtil testUtil;
  private static Server server;

  @BeforeAll
  static void init() throws Exception {
    illegalArgExceptionHandler = spyHandler((ex, req, res) -> {
      assertThat(ex.getMessage()).isEqualTo(ILLEGAL_ARG_ERROR_THROWN);
      assertThat(ex).isInstanceOf(IllegalArgumentException.class);
      res.status(400);
      res.type("plain/text");
      res.body(ILLEGAL_ARG_ERROR_HANDLED);
    });

    unexpectedExceptionHandler = spyHandler((ex, req, res) -> {
      assertThat(ex.getMessage()).isEqualTo(UNEXPECTED_ERROR_THROWN);
      assertThat(ex).isInstanceOf(RuntimeException.class);
      res.status(500);
      res.type("plain/text");
      res.body(UNEXPECTED_ERROR_HANDLED);
    });

    server = createTestServer(() -> {
      get(SUCCESS_PATH, SparkUtil.withExceptionHandlers((req, res) -> {
        res.status(200);
        res.type("plain/text");
        return SUCCESS_MSG;
      }).exception(IllegalArgumentException.class, illegalArgExceptionHandler)
          .exception(Exception.class, unexpectedExceptionHandler));

      get(ILLEGAL_ARG_ERROR_PATH, SparkUtil.withExceptionHandlers((req, res) -> {
        throw new IllegalArgumentException(ILLEGAL_ARG_ERROR_THROWN);
      }).exception(IllegalArgumentException.class, illegalArgExceptionHandler)
          .exception(Exception.class, unexpectedExceptionHandler));

      get(UNEXPECTED_ERROR_PATH, SparkUtil.withExceptionHandlers((req, res) -> {
        throw new RuntimeException(UNEXPECTED_ERROR_THROWN);
      }).exception(IllegalArgumentException.class, illegalArgExceptionHandler)
          .exception(Exception.class, unexpectedExceptionHandler));

      get(NOT_HANDLED_ERROR_PATH, SparkUtil.withExceptionHandlers((req, res) -> {
        throw new Error(NOT_HANDLED_ERROR_THROWN);
      }).exception(IllegalArgumentException.class, illegalArgExceptionHandler)
          .exception(Exception.class, unexpectedExceptionHandler));
    });
    testUtil = new SparkTestUtil(server.getURI().getPort());
  }

  @AfterAll
  static void tearDown() throws Exception {
    server.stop();
  }

  @Test
  @SuppressWarnings("unchecked")
  void routeWithExceptionHandlers_successRequest() throws Exception {
    clearInvocations(illegalArgExceptionHandler, unexpectedExceptionHandler);

    UrlResponse response = testUtil.doMethod("GET", SUCCESS_PATH, null, "plain/text");

    verifyZeroInteractions(illegalArgExceptionHandler);
    verifyZeroInteractions(unexpectedExceptionHandler);
    assertThat(response.status).isEqualTo(200);
    assertThat(response.body).contains(SUCCESS_MSG);
  }

  @Test
  @SuppressWarnings("unchecked")
  void routeWithExceptionHandlers_illegalArgHandledError() throws Exception {
    clearInvocations(illegalArgExceptionHandler, unexpectedExceptionHandler);

    UrlResponse response = testUtil.doMethod("GET", ILLEGAL_ARG_ERROR_PATH, null, "plain/text");

    verify(illegalArgExceptionHandler).handle(any(IllegalArgumentException.class), any(), any());
    verifyZeroInteractions(unexpectedExceptionHandler);
    assertThat(response.status).isEqualTo(400);
    assertThat(response.body).contains(ILLEGAL_ARG_ERROR_HANDLED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void routeWithExceptionHandlers_unexpectedHandledError() throws Exception {
    clearInvocations(illegalArgExceptionHandler, unexpectedExceptionHandler);

    UrlResponse response = testUtil.doMethod("GET", UNEXPECTED_ERROR_PATH, null, "plain/text");

    verify(unexpectedExceptionHandler).handle(any(RuntimeException.class), any(), any());
    verifyZeroInteractions(unexpectedExceptionHandler);
    assertThat(response.status).isEqualTo(500);
    assertThat(response.body).contains(UNEXPECTED_ERROR_HANDLED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void routeWithExceptionHandlers_notHandledError() throws Exception {
    clearInvocations(illegalArgExceptionHandler, unexpectedExceptionHandler);

    UrlResponse response = testUtil.doMethod("GET", NOT_HANDLED_ERROR_PATH, null, "plain/text");

    verifyZeroInteractions(illegalArgExceptionHandler);
    verifyZeroInteractions(unexpectedExceptionHandler);
    assertThat(response.status).isEqualTo(500);
    assertThat(response.body)
        .as("Default Spark error handler should handle it")
        .doesNotContain(ILLEGAL_ARG_ERROR_THROWN, UNEXPECTED_ERROR_THROWN, NOT_HANDLED_ERROR_THROWN);
  }

  private static Server createTestServer(SparkApplication sparkApplication) throws Exception {
    SparkWebApp webApp = new SparkWebApp(0, sparkApplication);

    Server server = webApp.getServer();
    server.start();
    SparkTestUtil testUtil = new SparkTestUtil(server.getURI().getPort());

    RetryPolicy retryPolicy = new RetryPolicy()
        .retryOn(Exception.class)
        .withDelay(100, TimeUnit.MILLISECONDS)
        .withMaxRetries(40);

    Failsafe.with(retryPolicy).run(() -> probeServer(testUtil));
    return server;
  }

  private static void probeServer(SparkTestUtil testUtil) throws Exception {
    UrlResponse response = testUtil.doMethod("GET", SUCCESS_PATH, null, "plain/text");
    if (response.status != 200) {
      throw new IOException("Invalid response code: " + response.status);
    }
  }

  private static <T extends Exception> ExceptionHandler<T> spyHandler(ExceptionHandler<T> handler) {
    return spy(new MockableHandler<>(handler));
  }

  private static class MockableHandler<T extends Exception> implements ExceptionHandler<T> {

    private final ExceptionHandler<T> handler;

    private MockableHandler(ExceptionHandler<T> handler) {
      this.handler = handler;
    }

    @Override
    public void handle(T exception, spark.Request request, Response response) {
      handler.handle(exception, request, response);
    }
  }
}
