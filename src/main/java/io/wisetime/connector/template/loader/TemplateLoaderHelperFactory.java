/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

/**
 * Factory for {@link TemplateLoaderHelper} that will choose appropriate implementation based on schema.
 * To specify path to template located on classpath use classpath:template-file.ftl.
 * To specify path to template saved in file system use absolute path, e.g. /var/jenkins/template.ftl.
 *
 * @author vadym
 * @see ClasspathTemplateLoaderHelper
 * @see FileTemplateLoaderHelper
 */
public class TemplateLoaderHelperFactory {

  /**
   * @return appropriate implementation for provided template path
   */
  public static TemplateLoaderHelper from(String path) {
    if (path.startsWith("classpath:")) {
      return new ClasspathTemplateLoaderHelper(path);
    }
    return new FileTemplateLoaderHelper(path);
  }
}
