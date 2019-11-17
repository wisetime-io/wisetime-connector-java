/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.wisetime.connector.api_client.PostResult.PostResultStatus;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.time_poster.deduplication.TimeGroupIdStore;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.api_client.JsonPayloadService;
import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.TimeGroup;
import spark.ExceptionHandler;
import spark.ExceptionHandlerImpl;
import spark.ExceptionMapper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;
import static spark.Spark.stop;

/**
 * Spark web application that implements WiseTime posted time web hook.
 *
 * @author thomas.haines
 * @see <a href="http://sparkjava.com">Spark</a> for more infromation about Spark framework.
 */
class WebhookApplication implements SparkApplication {

  private static final Logger log = LoggerFactory.getLogger(WebhookApplication.class);

  static final String PING_RESPONSE = "pong";
  static final String UNEXPECTED_ERROR = "Unexpected error";
  static final String MESSAGE_KEY = "message";

  private final JsonPayloadService payloadService;
  private final WiseTimeConnector wiseTimeConnector;
  private final MetricService metricService;
  private final TimeGroupIdStore timeGroupIdStore;

  WebhookApplication(JsonPayloadService payloadService, WiseTimeConnector wiseTimeConnector,
                     MetricService metricService, TimeGroupIdStore timeGroupIdStore) {
    this.payloadService = payloadService;
    this.wiseTimeConnector = wiseTimeConnector;
    this.metricService = metricService;
    this.timeGroupIdStore = timeGroupIdStore;
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
      return payloadService.write(metricService.getMetrics());
    });

    if (wiseTimeConnector == null) {
      throw new UnsupportedOperationException("WiseTime Connector was not configured in server builder");
    }

    post("/receiveTimePostedEvent", (request, response) -> {
      final TimeGroup timeGroup = payloadService.read(request.body(), TimeGroup.class);
      log.debug("Received {} for posting time", timeGroup);
      Optional<PostResult> timeGroupStatus = timeGroupIdStore.alreadySeenWebHook(timeGroup.getGroupId());
      // we only care to deduplicate successful time posts. No harm in retrying failures
      if (timeGroupStatus.isPresent() && PostResultStatus.SUCCESS == timeGroupStatus.get().getStatus()) {
        return payloadService.writeWithInfo(MESSAGE_KEY, getMessageBasedOnResult(timeGroupStatus.get()));
      }
      Optional<String> callerKey = RuntimeConfig.getString(ConnectorConfigKey.CALLER_KEY);
      final PostResult postResult;
      if (callerKey.isPresent() && !callerKey.get().equals(timeGroup.getCallerKey())) {
        postResult = PostResult.PERMANENT_FAILURE()
            .withMessage("Invalid caller key in posted time webhook call");
      } else {
        postResult = wiseTimeConnector.postTime(request, timeGroup);
      }
      if (PostResultStatus.SUCCESS == postResult.getStatus()) {
        timeGroupIdStore.putTimeGroupId(timeGroup.getGroupId(), postResult.getStatus().name(),
            postResult.getMessage().orElse(""));
      }
      log.info("Posted time group {}, result: {}", timeGroup.getGroupId(), postResult);

      response.type("application/json");
      response.status(postResult.getStatus().getStatusCode());
      String message = getMessageBasedOnResult(postResult);
      return payloadService.writeWithInfo(MESSAGE_KEY, message);
    });
  }

  private String getMessageBasedOnResult(PostResult postResult) {
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
  }

  private void addExceptionMapping() {
    ExceptionHandler<Exception> badRequestHandler = (ex, req, res) -> {
      log.error("Invalid request to {} with error {}", req.pathInfo(), ex.getMessage());
      res.status(400);
      res.type("application/json");
      res.body(createErrorPayload("Invalid request"));
    };

    exception(JsonParseException.class, badRequestHandler);
    exception(JsonMappingException.class, badRequestHandler);
    exception(Exception.class, (ex, req, res) -> {
      log.error("Unhandled exception requesting {}", req.pathInfo(), ex);
      res.status(500);
      res.type("application/json");
      res.body(createErrorPayload(UNEXPECTED_ERROR));
    });
  }

  private String createErrorPayload(String errorMessage) {
    try {
      return payloadService.writeWithInfo(MESSAGE_KEY, errorMessage);
    } catch (JsonProcessingException e) {
      log.error("Couldn't create response payload", e);
      return String.format("{\"%s\":%s}", MESSAGE_KEY, errorMessage);
    }
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

  @Override
  public void destroy() {
    stop();
  }
}
