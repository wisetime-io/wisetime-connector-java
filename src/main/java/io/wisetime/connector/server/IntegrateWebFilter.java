/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.server;

import javax.servlet.FilterConfig;

import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;

/**
 * Filter to bind {@link SparkApplication} to server.
 *
 * @author thomas.haines@practiceinsight.io
 */
public class IntegrateWebFilter extends SparkFilter {

  private final SparkApplication sparkApp;

  public IntegrateWebFilter(SparkApplication sparkApp) {
    this.sparkApp = sparkApp;
  }

  @Override
  protected SparkApplication[] getApplications(final FilterConfig filterConfig) {
    SparkApplication[] sparkAppArr = new SparkApplication[1];
    sparkAppArr[0] = sparkApp;
    return sparkAppArr;
  }
}
