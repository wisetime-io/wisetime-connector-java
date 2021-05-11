/*
 * Copyright (c) 2021 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.adjunct_keywords;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author thomas.haines
 */
@Data
@Accessors(chain = true)
public class KeywordUpdate {

  private TagId tagId = new TagId();
  private Set<String> additionalKeywords = new HashSet<>();

}
