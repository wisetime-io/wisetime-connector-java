/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All rights reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModelException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author alvin.llobrera@practiceinsight.io
 */
class ActivityTimeUTCFormatTest {

  @Test
  void formatToPlainText() throws TemplateModelException {
    ActivityTimeUTCFormat activityTimeUTCFormat = new ActivityTimeUTCFormat();
    assertThat(activityTimeUTCFormat.formatToPlainText(new SimpleNumber(201906040900L)))
        .isEqualTo("2019-06-04T09:00:00Z");
  }

  @Test
  void formatToPlainText_invalid() throws TemplateModelException {
    ActivityTimeUTCFormat activityTimeUTCFormat = new ActivityTimeUTCFormat();
    assertThat(activityTimeUTCFormat.formatToPlainText(new SimpleNumber(201906048989L)))
        .as("argument passed is not a valid date in yyyyMMddHHmm format")
        .isEqualTo("Unknown");
  }
}