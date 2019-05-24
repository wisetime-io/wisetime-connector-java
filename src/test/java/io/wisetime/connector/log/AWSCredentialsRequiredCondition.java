/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import java.io.File;

/**
 * JUnit5 condition that checks if ${HOME}/aws/credentials and ${HOME}/aws/config is present
 */
class AWSCredentialsRequiredCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (AnnotationUtils.findAnnotation(context.getElement(), AWSCredentialsRequired.class).isPresent()) {
      File awsHome = new File(System.getProperty("user.home"), ".aws");
      File awsCredentials = new File(awsHome, "credentials");
      File awsConfig = new File(awsHome, "config");
      if (!awsCredentials.exists()) {
        return ConditionEvaluationResult.disabled(
            "AWS credentials file " + awsCredentials.getAbsolutePath() + " does not exist."
        );
      }
      if (!awsConfig.exists()) {
        return ConditionEvaluationResult.disabled("AWS config file " + awsConfig.getAbsolutePath() + " does not exist.");
      }
      return ConditionEvaluationResult.enabled("AWS config files exist.");
    }
    return ConditionEvaluationResult
        .enabled("condition only applies to tests annotated with @" + AWSCredentialsRequired.class.getSimpleName());
  }
}
