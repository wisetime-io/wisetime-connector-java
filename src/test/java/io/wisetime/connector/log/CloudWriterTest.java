/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.wisetime.connector.config.PropertyFileUtil;
import java.net.URL;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
 */
class CloudWriterTest {

  @Test
  void addUserPropertyFile() {
    URL filePath = TestFileUtil.class.getResource("/user_defined.properties");
    String pathToFile = filePath.getPath();

    Map<String, String> result = PropertyFileUtil.addUserPropertyFile(pathToFile);

    assertThat(result.size())
        .as("property file contains two keys")
        .isEqualTo(2);

    assertThat(result.get("any_config"))
        .isEqualTo("is-included");
    assertThat(result.get("multi_item"))
        .isEqualTo("yes");

  }

}
