/*
 * Copyright (c) 2020 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.wisetime.connector.log.TestFileUtil;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
 */
class JsonMapUtilTest {

  @Test
  void parseRootJsonChildrenToMap() {
    String jsonMap = TestFileUtil.getFile("/propertyMap.json");
    final Map<String, String> result = JsonMapUtil.parseRootJsonChildrenToMap(jsonMap);

    assertThat(result.get("json_item_a"))
        .isEqualTo("ONE");
    assertThat(result.get("json_item_b"))
        .isEqualTo("TWO");
  }
}