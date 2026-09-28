/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import static org.assertj.core.api.Assertions.assertThat;

import freemarker.cache.TemplateLoader;
import java.io.BufferedReader;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author vadym
 */
public class ClasspathTemplateLoaderHelperTest {

  private ClasspathTemplateLoaderHelper loaderHelper;

  @BeforeEach
  public void setup() {
    loaderHelper = new ClasspathTemplateLoaderHelper("classpath:freemarker-template/test-template.ftl");
  }

  @Test
  public void createTemplateLoader() throws IOException {
    TemplateLoader templateLoader = loaderHelper.createTemplateLoader();
    Object templateSource = templateLoader.findTemplateSource(loaderHelper.getTemplateName());
    try (BufferedReader reader = new BufferedReader(templateLoader.getReader(templateSource, "UTF-8"))) {
      assertThat(reader.readLine())
          .as("compare first line of template")
          .isEqualTo("${getGroupName()} [${getTotalDurationSecs()?string.@duration}]");
    }
  }

  @Test
  public void getTemplateName() {
    assertThat(loaderHelper.getTemplateName())
        .isEqualTo("freemarker-template/test-template.ftl");
  }
}
