/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.File;
import java.io.IOException;

/**
 * {@link TemplateLoaderHelper} implementation for file system based template. In template path is invalid
 * {@link IllegalStateException} will be thrown.
 *
 * @author vadym
 * @see TemplateLoaderHelperFactory
 */
public class FileTemplateLoaderHelper implements TemplateLoaderHelper {

  private final File template;

  public FileTemplateLoaderHelper(String path) {
    File template = new File(path);
    if (!template.exists()) {
      throw new IllegalArgumentException("Failed to load activity text template from file: " + path);
    }
    this.template = template;
  }

  @Override
  public TemplateLoader createTemplateLoader() {
    try {
      return new FileTemplateLoader(template.getParentFile());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create file template loader", e);
    }
  }

  @Override
  public String getTemplateName() {
    return template.getName();
  }
}
