/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.Tag;
import io.wisetime.generated.connect.TimeGroup;
import spark.ExceptionHandler;
import spark.ExceptionHandlerImpl;
import spark.ExceptionMapper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import static io.wisetime.connector.api_client.PostResult.PostResultStatus;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.stop;

/**
 * Spark web application that implements WiseTime posted time web hook.
 *
 * @author thomas.haines@practiceinsight.io
 * @see <a href="http://sparkjava.com">Spark</a> for more infromation about Spark framework.
 */
class WebhookApplication implements SparkApplication {

  private static final Logger log = LoggerFactory.getLogger(WebhookApplication.class);
  static final String PING_RESPONSE = "pong";
  static final String UNEXPECTED_ERROR = "Unexpected error";

  private final ObjectMapper om;
  private final WiseTimeConnector wiseTimeConnector;
  private final MetricService metricService;

  WebhookApplication(ObjectMapper objectMapper, WiseTimeConnector wiseTimeConnector, MetricService metricService) {
    this.wiseTimeConnector = wiseTimeConnector;
    this.metricService = metricService;
    om = objectMapper;
  }

  @Override
  public void init() {
    staticFileLocation("/public");
    addEndpoints();
    addExceptionMapping();
  }

  /**
   * Invoked from the SparkFilter. Add routes here.
   */
  private void addEndpoints() {
    get("/", (rq, rs) -> new ModelAndView(new HashMap(), "home"), new ThymeleafTemplateEngine());

    get("/ping", (rq, rs) -> {
      rs.type("plain/text");
      return PING_RESPONSE;
    });

    get("/metric", (rq, rs) -> {
      rs.type("application/json");
      return om.writeValueAsString(metricService.getMetrics());
    });

    if (wiseTimeConnector == null) {
      throw new UnsupportedOperationException("WiseTime Connector was not configured in server builder");
    }

    post("/receiveTimePostedEvent", (request, response) -> {
      final TimeGroup timeGroup = om.readValue(request.body(), TimeGroup.class);
      final PostResult postResult = wiseTimeConnector.postTime(request, timeGroup);
      log(timeGroup, postResult);

      response.type("plain/text");
      response.status(postResult.getStatus().getStatusCode());
      switch (postResult.getStatus()) {
        case SUCCESS:
          return "Success";
        case PERMANENT_FAILURE:
        case TRANSIENT_FAILURE:
          return postResult.getMessage().orElse(UNEXPECTED_ERROR);
        default:
          log.warn("Unexpected posting time result: {}", postResult.getStatus());
          return UNEXPECTED_ERROR;
      }
    });
  }

  private void addExceptionMapping() {
    ExceptionHandler<Exception> badRequestHandler = (ex, req, res) -> {
      log.error("Invalid request to {} with error {}", req.pathInfo(), ex.getMessage());
      res.status(400);
      res.type("plain/text");
      res.body("Invalid request");
    };

    exception(JsonParseException.class, badRequestHandler);
    exception(JsonMappingException.class, badRequestHandler);
    exception(Exception.class, (ex, req, res) -> {
      log.error("Unhandled exception requesting " + req.pathInfo(), ex);
      res.status(500);
      res.type("plain/text");
      res.body(UNEXPECTED_ERROR);
    });
  }

  /**
   * Maps an exception handler to be executed when an exception occurs during routing. Copied from {@link
   * spark.Service#exception(Class, ExceptionHandler)} But modified to use the Servlet instance of ExceptionMapper.
   * Details here: https://github.com/perwendel/spark/issues/1062
   *
   * @param exceptionClass the exception class
   * @param handler        The handler
   */
  private <T extends Exception> void exception(Class<T> exceptionClass, ExceptionHandler<? super T> handler) {
    // wrap
    ExceptionHandlerImpl wrapper = new ExceptionHandlerImpl<T>(exceptionClass) {
      @Override
      public void handle(T exception, Request request, Response response) {
        handler.handle(exception, request, response);
      }
    };

    ExceptionMapper.getServletInstance().map(exceptionClass, wrapper);
  }

  private void log(final TimeGroup timeGroup, final PostResult postResult) {
    final String message = String.format(
        "[%s] posting time on behalf of [%s] with tags [%s]%s",
        StringUtils.capitalize(postResult.name().replaceAll("_", " ").toLowerCase()),
        timeGroup.getUser().getName(),
        timeGroup.getTags().stream().map(Tag::getName).collect(Collectors.joining(", ")),
        postResult.getMessage().map(m -> ": " + m).orElse("")
    );

    final BiConsumer<String, Optional<Throwable>> logError = (description, throwable) -> {
      if (throwable.isPresent()) {
        log.error(description, throwable.get());
      } else {
        log.error(description);
      }
    };

    if (postResult.getStatus() == PostResultStatus.SUCCESS) {
      log.info(message);
    } else {
      logError.accept(message, postResult.getError());
    }
  }

  @Override
  public void destroy() {
    stop();
  }
}
