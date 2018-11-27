/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import com.google.common.collect.Lists;

import com.github.javafaker.Faker;

import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines@practiceinsight.io
 */
class RestRequestExecutorTest {
  private String apiKey;
  private RestRequestExecutor restExec;

  @BeforeEach
  void setup() {
    apiKey = new Faker().numerify("apiKey######");
    restExec = new RestRequestExecutor(apiKey);
  }

  @Test
  void mergeNamedParams() {
    assertThat(restExec.mergeNamedParams("foo", Lists.newArrayList()))
        .isEqualTo("foo");

    assertThat(
        restExec.mergeNamedParams(
            "/:foo/goo",
            Lists.newArrayList(new BasicHeader("foo", "bar"))))
        .isEqualTo("/bar/goo");
  }

  @Test
  void mergeMulti() {
    assertThat(
        restExec.mergeNamedParams(
            "/:foot/:foo",
            Lists.newArrayList(
                new BasicHeader("foo", "bar"),
                new BasicHeader("foot", "hand"))))
        .isEqualTo("/hand/bar");
  }
}
