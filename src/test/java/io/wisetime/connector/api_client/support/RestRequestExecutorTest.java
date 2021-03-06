/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javafaker.Faker;
import com.google.common.collect.Lists;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author thomas.haines
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
  void mergeNamedParams_withSpace() {
    assertThat(
        restExec.mergeNamedParams(
            "/:foo/goo",
            Lists.newArrayList(new BasicHeader("foo", "bar baz"))))
        .isEqualTo("/bar%20baz/goo");
  }

  @Test
  void mergeNamedParams_withSlash() {
    assertThat(
        restExec.mergeNamedParams(
            "/:foo/goo",
            Lists.newArrayList(new BasicHeader("foo", "bar/baz"))))
        .isEqualTo("/bar%2Fbaz/goo");
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
