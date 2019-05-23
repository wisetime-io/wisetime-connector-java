/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class CloudWriterTest {

  @Test
  void addUserPropertyFile() {
    URL filePath = TestFileUtil.class.getResource("/user_defined.properties");
    String pathToFile = filePath.getPath();

    Map<String, String> result = CloudWriter.addUserPropertyFile(pathToFile);

    assertThat(result.size())
        .as("property file contains two keys")
        .isEqualTo(2);

    assertThat(result.get("any_config"))
        .isEqualTo("is-included");
    assertThat(result.get("multi_item"))
        .isEqualTo("yes");

  }

}
