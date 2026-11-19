/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import io.wisetime.connector.api_client.SyncScope;
import io.wisetime.connector.datastore.ConnectorStore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author thomas.haines
 */
@Data
@RequiredArgsConstructor
@Accessors(chain = true)
public class KeywordConfig {

  private final ConnectorStore connectStore;
  private final SyncScope syncScope;

  private int batchSize = 100;

}
