/*
 * Copyright (c) 2023 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.concise;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import io.wisetime.generated.connect.TimeGroup;
import io.wisetime.generated.connect.TimeRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConciseFormatter {

  private final ConciseConfig config;

  public ConciseFormatter(ConciseConfig config) {
    this.config = config;
    int validMinLen = config.getTruncatedSuffix().length() + 1;
    Preconditions.checkState(config.getMaxDescriptionLen() >= validMinLen,
        "MaxDescriptionLen must >=%s (length was %s)",
        validMinLen,
        config.getMaxDescriptionLen());
  }

  public String createDescription(TimeGroup timeGroup) {
    if (timeGroup.getDescription() != null && timeGroup.getDescription().trim().length() > 0) {
      log.debug("using group description");
      return combineAndTrim(List.of(timeGroup.getDescription().trim()));
    }

    List<String> timeTitles = new ArrayList<>();
    if (timeGroup.getTimeRows() != null) {
      timeGroup.getTimeRows().stream()
          .map(TimeRow::getDescription)
          .filter(Objects::nonNull)
          .filter(describe -> !describe.isEmpty())
          .forEach(timeTitles::add);
    }

    return combineAndTrim(timeTitles);
  }

  public String combineAndTrim(List<String> descriptionTexts) {
    if (descriptionTexts.isEmpty()) {
      log.debug("no matter found for description, returning empty string");
      return "";
    }
    String description = Joiner.on(config.getDelimiter())
        .skipNulls()
        .join(descriptionTexts);
    if (description.length() <= config.getMaxDescriptionLen()) {
      // within expected bounds
      return description;
    }

    int truncLength = config.getMaxDescriptionLen() - config.getTruncatedSuffix().length();
    return description.substring(0, truncLength) + config.getTruncatedSuffix();
  }

}
