/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template;

/**
 * @author shane.xie@practiceinsight.io
 */
public class TemplateProcessingException extends Exception {

  public TemplateProcessingException(final String message, final Throwable cause) {
    new Exception(message, cause);
  }
}
