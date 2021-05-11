/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Either externalId or path+name should be set.
 */
@Data
@Accessors(chain = true)
public class TagId {

  private String externalId;
  private String name;
  private String path;

}
