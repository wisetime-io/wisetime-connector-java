/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.logging;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.util.Arrays;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * @author galya.bogdanova@staff.wisetime.io
 */
public class WtTurboFilter extends TurboFilter {

  private MessagePublisher messagePublisher;

  public WtTurboFilter(MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  @Override
  @SuppressWarnings("ParameterNumber")
  public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params,
                            Throwable throwable) {
    if (StringUtils.isBlank(format) && throwable == null) {
      return FilterReply.ACCEPT;
    }
    WtLog.Level wtLevel;
    if (level == Level.ERROR) {
      wtLevel = WtLog.Level.ERROR;
    } else if (level == Level.WARN) {
      wtLevel = WtLog.Level.WARN;
    } else {
      wtLevel = WtLog.Level.INFO;
    }

    String msg = getFormattedLog(format, params);
    if (throwable != null) {
      msg += throwable.getMessage() + " " + Arrays.toString(throwable.getStackTrace());
    }
    messagePublisher.publish(new WtLog(wtLevel, Thread.currentThread().getName(), msg));
    return FilterReply.ACCEPT;
  }

  @VisibleForTesting
  String getFormattedLog(String format, Object[] params) {
    if (StringUtils.isBlank(format)) {
      return "";
    }
    final String formattedMessage = ArrayUtils.isNotEmpty(params) ?
        MessageFormatter.arrayFormat(format, params).getMessage() : format;
    return StringUtils.isNotBlank(formattedMessage) ? formattedMessage : "";
  }
}
