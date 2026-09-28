/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.format;

import static org.assertj.core.api.Assertions.assertThat;

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateModelException;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
class PrintSubmittedDateFormatTest {

  @Test
  void formatToPlainText() throws TemplateModelException {
    PrintSubmittedDateFormat format = new PrintSubmittedDateFormat("HH:mm");
    assertThat(format.formatToPlainText(new SimpleNumber(20181123105350983L)))
        .isEqualTo("10:53");
  }
}
