/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import java.io.IOException;

/**
 * @author thomas.haines
 */
public interface KeywordConsumer {

  /**
   * Note, This method does not guarantee that all keywords sent to it have been persisted.
   */
  void persistKeywordAsync(KeywordUpdate keywordUpdate) throws IOException;

  /**
   * This method guarantees that all keywords sent to the consumer have been persisted.
   */
  void flushUploadQueue() throws IOException;

}
