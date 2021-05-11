/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

/**
 * @author thomas.haines
 */
public interface KeywordConsumer {

  boolean persistKeyword(KeywordUpdate keywordUpdate);

  default void flushUploadQueue() {
  }

}
