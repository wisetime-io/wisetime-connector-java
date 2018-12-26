/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

import io.wisetime.connector.api_client.PostResult;
import io.wisetime.connector.config.TolerantObjectMapper;
import io.wisetime.connector.integrate.WiseTimeConnector;
import io.wisetime.generated.connect.TimeGroup;
import spark.ModelAndView;
import spark.servlet.SparkApplication;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

/**
 * Spark application that handles time posted event and basic UI.
 *
 * @author thomas.haines@practiceinsight.io
 * @see <a href="http://sparkjava.com">Spark</a> for more infromation about Spark framework.
 */
public class IntegrateApplication implements SparkApplication {

  public static final String PING_RESPONSE = "pong";
  private final ObjectMapper om;
  private final WiseTimeConnector wiseTimeConnector;

  IntegrateApplication(WiseTimeConnector wiseTimeConnector) {
    this.wiseTimeConnector = wiseTimeConnector;
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

    // endpoint re posted time
    if (wiseTimeConnector == null) {
      throw new UnsupportedOperationException("time poster was not configured in server builder");
    }

    post("/receiveTimePostedEvent", (request, response) -> {
      TimeGroup userPostedTime = om.readValue(request.body(), TimeGroup.class);

      PostResult postResult = wiseTimeConnector.postTime(request, userPostedTime);

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
          // If we don't get a recognized PostResult, this is an unexpected error
          response.status(500);
          return "Unexpected error";
      }
    });
  }

  @Override
  public void destroy() {
  }
}
