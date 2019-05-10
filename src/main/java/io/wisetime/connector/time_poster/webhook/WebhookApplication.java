/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.time_poster.webhook;

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
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.metric.MetricService;
import io.wisetime.generated.connect.Tag;
import io.wisetime.generated.connect.TimeGroup;
import spark.ModelAndView;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

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

  private final ObjectMapper om;
  private final WiseTimeConnector wiseTimeConnector;
  private final MetricService metricService;

  WebhookApplication(WiseTimeConnector wiseTimeConnector, MetricService metricService) {
    this.wiseTimeConnector = wiseTimeConnector;
    this.metricService = metricService;
    om = TolerantObjectMapper.create();
  }

  @Override
  public void init() {
    staticFileLocation("/public");
    addEndpoints();
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
      switch (postResult) {
        case SUCCESS:
          response.status(200);
          return "Success";
        case PERMANENT_FAILURE:
          response.status(400);
          return "Invalid request";
        case TRANSIENT_FAILURE:
        default:
          response.status(500);
          return "Unexpected error";
      }
    });
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

    if (postResult == PostResult.SUCCESS) {
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
