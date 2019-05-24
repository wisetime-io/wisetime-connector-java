/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

/**
 * <pre>
 *  When filter is enabled, you can define a minimum level,
 *  For example, if default logging set to INFO, but you want only WARN logging to cloud host BUT FOR (except) if package
 *  prefix was org.foo or org.bar, use:
 *    &lt;filter class="io.wisetime.wise_log_aws.cloud.ThresholdFilterWithExclusion"&gt;
 *       &lt;level&gt;WARN&lt;/level&gt;
 *       &lt;excludedLogPrefixList&gt;org.foo;org.bar&lt;/excludedLogPrefixList&gt;
 *     &lt;/filter&gt;
 *  Separate items with ';' character.
 * </pre>
 */
public class ThresholdFilterWithExclusion extends ThresholdFilter {

  private List<String> excludedLogPrefixList = new ArrayList<>();

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!isStarted()) {
      return FilterReply.NEUTRAL;
    }

    if (event == null || event.getLevel() == null) {
      return FilterReply.NEUTRAL;
    }

    final FilterReply thresholdResult = super.decide(event);
    if (thresholdResult == FilterReply.DENY && isExcludedPackagePrefix(event.getLoggerName())) {
      return FilterReply.NEUTRAL;
    }

    return thresholdResult;
  }

  private boolean isExcludedPackagePrefix(String loggerName) {
    if (loggerName == null) {
      // no logger name, never exclude
      return false;
    }

    final String logNameLower = loggerName.toLowerCase();
    return excludedLogPrefixList.stream().anyMatch(logNameLower::startsWith);
  }

  @SuppressWarnings("WeakerAccess")
  public void setExcludedLogPrefixList(final String excludedPrefixTsv) {
    this.excludedLogPrefixList = new ArrayList<>();
    if (excludedPrefixTsv == null) {
      return;
    }
    String tsvList = excludedPrefixTsv.trim();
    // remove all spaces
    tsvList = tsvList.replaceAll("\\s+", "");

    if (tsvList.isEmpty()) {
      return;
    }

    String[] packageNames = tsvList.split(";");
    if (packageNames.length == 0) {
      return;
    }

    this.excludedLogPrefixList = Arrays.stream(packageNames)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
  }

}
