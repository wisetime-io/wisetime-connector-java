/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author vadym
 */
public class TemplateLoaderHelperFactoryTest {

  @Test
  public void from_classPath() {
    assertThat(TemplateLoaderHelperFactory.from("classpath:template.ftml"))
        .isInstanceOf(ClasspathTemplateLoaderHelper.class);
  }

  @Test
  public void from_file() throws IOException {
    File tempFile = File.createTempFile("temp", ".ftml");
    try {
      assertThat(TemplateLoaderHelperFactory.from(tempFile.getAbsolutePath()))
          .isInstanceOf(FileTemplateLoaderHelper.class);
    } finally {
      tempFile.delete();
    }
  }

  @Test
  public void from_invalidPath() {
    assertThatThrownBy(() -> TemplateLoaderHelperFactory.from("/" + UUID.randomUUID().toString()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to load activity text template from file:");
  }
}