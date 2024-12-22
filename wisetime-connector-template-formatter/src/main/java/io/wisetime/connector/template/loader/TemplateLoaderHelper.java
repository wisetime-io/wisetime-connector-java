/*
 * Copyright (c) 2017 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.template.loader;

import freemarker.cache.TemplateLoader;

/**
 * Freemarker template loader helper utility. It is responsible for creating appropriate {@link TemplateLoader}
 * and formatting compliant template name from template path for loader.
 *
 * @author vadym
 */
public interface TemplateLoaderHelper {

  TemplateLoader createTemplateLoader();

  String getTemplateName();
}
