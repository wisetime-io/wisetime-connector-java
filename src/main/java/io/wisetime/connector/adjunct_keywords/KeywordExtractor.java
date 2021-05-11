/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

/**
 * Used to support keyword augmenters such as document management file ids and the like.
 *
 * @author thomas.haines
 */
public interface KeywordExtractor {

  void fetchKeywords(KeywordConfig keywordConf, KeywordConsumer keywordConsumer);

  /**
   * Default no-op close method, called if connector is re-created.
   */
  default void close() {

  }
}
