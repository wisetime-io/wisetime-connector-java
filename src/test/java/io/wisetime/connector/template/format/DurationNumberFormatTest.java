/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import freemarker.core.TemplateValueFormatException;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModelException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author vadym
 */
class DurationNumberFormatTest {

  @Test
  void formatToPlainText() throws TemplateModelException, TemplateValueFormatException {
    DurationNumberFormat numberFormatter = new DurationNumberFormat();
    assertThat(numberFormatter.formatToPlainText(new SimpleNumber(1000)))
        .isEqualTo("16m 40s");

    assertThat(numberFormatter.formatToPlainText(new SimpleNumber(5)))
        .isEqualTo("5s");

    assertThat(numberFormatter.formatToPlainText(new SimpleNumber(10000)))
        .isEqualTo("2h 46m 40s");
  }
}