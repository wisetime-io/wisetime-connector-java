/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.generated.connect.Tag;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.connector.logging.MessagePublisher;
import io.wisetime.connector.logging.WtEvent;
import spark.ModelAndView;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

/**
 * Spark web application that implements WiseTime posted time web hook.
 *
 * @author thomas.haines@practiceinsight.io
 * @see <a href="http://sparkjava.com">Spark</a> for more infromation about Spark framework.
 */
public class IntegrateApplication implements SparkApplication {

  private static final Logger log = LoggerFactory.getLogger(IntegrateApplication.class);
  public static final String PING_RESPONSE = "pong";
  private final ObjectMapper om;
  private final WiseTimeConnector wiseTimeConnector;
  private final MessagePublisher messagePublisher;

  IntegrateApplication(WiseTimeConnector wiseTimeConnector, MessagePublisher messagePublisher) {
    this.wiseTimeConnector = wiseTimeConnector;
    this.messagePublisher = messagePublisher;
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
          messagePublisher.publish(new WtEvent(WtEvent.Type.TIME_GROUP_POSTED));
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
  }
}
