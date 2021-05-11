/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import io.wisetime.connector.api_client.SyncScope;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author thomas.haines
 */
@Data
@Accessors(chain = true)
public class KeywordConfig {

  private SyncScope syncScope;

  private int batchSize = 100;

}
