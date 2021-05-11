/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import java.io.IOException;

/**
 * @author thomas.haines
 */
public interface KeywordConsumer {

  boolean persistKeyword(KeywordUpdate keywordUpdate) throws IOException;

  default void flushUploadQueue() throws IOException {
  }

}
