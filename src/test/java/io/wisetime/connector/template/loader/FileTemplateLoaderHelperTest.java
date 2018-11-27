/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author vadym
 */
public class FileTemplateLoaderHelperTest {

  private FileTemplateLoaderHelper fileTemplateLoaderHelper;
  private File template;

  @BeforeEach
  void setup() throws Exception {
    template = File.createTempFile("template", ".ftl");
    fileTemplateLoaderHelper = new FileTemplateLoaderHelper(template.getCanonicalPath());
  }

  @AfterEach
  void cleanup() {
    template.delete();
  }

  @Test
  void createTemplateLoader() throws Exception {
    Object templateSource = fileTemplateLoaderHelper.createTemplateLoader()
        .findTemplateSource(fileTemplateLoaderHelper.getTemplateName());

    assertThat(templateSource.toString())
        .contains(template.toString());
  }

  @Test
  void getTemplateName() {
    assertThat(fileTemplateLoaderHelper.getTemplateName())
        .isEqualTo(template.getName());
  }
}