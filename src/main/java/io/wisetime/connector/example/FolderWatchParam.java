/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.example;

import io.wisetime.connector.template.TemplateFormatterConfig;
import picocli.CommandLine;

/**
 * @author thomas.haines@practiceinsight.io
 */
class FolderWatchParam {

  @CommandLine.Option(names = {"--apiKey", "--key", "-k"}, description = "api key when using the default api client")
  String apiKey = "";

  @CommandLine.Option(names = {"--watchDir", "-d", "--dir"}, required = true,
      description = "folder to watch for new files (which are uploaded as tags)")
  String watchFolder;

  @CommandLine.Option(names = {"--useWinClr"}, defaultValue = TemplateFormatterConfig.DEFAULT_USE_WINCLR + "",
      description = "if set - template formatter will generate output with Windows-style newline delimiter: \\r\\n,"
          + "default is ${DEFAULT-VALUE}")
  boolean templateUseWinClr;

  @CommandLine.Option(names = {"--template", "-t"}, defaultValue = TemplateFormatterConfig.DEFAULT_TEMPLATE_PATH,
      description = "custom Freemarker template path to use. E.g. /var/jenkins/template.ftl, "
          + "default is ${DEFAULT-VALUE}")
  String templatePath;

  @CommandLine.Option(names = {"--maxDescriptionLength"}, defaultValue = TemplateFormatterConfig.DEFAULT_MAX_LENGTH + "",
      description = "max length of template formatter output. Use 0 or negative number to indicate no limit,"
          + "default is ${DEFAULT-VALUE}")
  int templateMaxLength;

  String getApiKey() {
    return apiKey;
  }

  String getWatchFolder() {
    return watchFolder;
  }

  public boolean isTemplateUseWinClr() {
    return templateUseWinClr;
  }

  public String getTemplatePath() {
    return templatePath;
  }

  public int getTemplateMaxLength() {
    return templateMaxLength;
  }
}
