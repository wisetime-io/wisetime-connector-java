/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Implementation for {@link TemplateLoaderHelper} to manage freemarker templates provided from classpath. Path
 * expected to start with `classpath:` schema, e.g.: classpath:default-template.ftl or
 * classpath:template-folder/my-template.ftl.
 *
 * @author vadym
 * @see TemplateLoaderHelperFactory
 */
public class ClasspathTemplateLoaderHelper implements TemplateLoaderHelper {

  private final String templateClasspath;

  public ClasspathTemplateLoaderHelper(String templateClasspath) {
    this.templateClasspath = templateClasspath;
  }

  @Override
  public TemplateLoader createTemplateLoader() {
    return new ClassTemplateLoader(getClass().getClassLoader(), "");
  }

  @Override
  public String getTemplateName() {
    return templateClasspath.substring("classpath:".length());
  }
}
