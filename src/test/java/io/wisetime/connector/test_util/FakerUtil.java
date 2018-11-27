/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.test_util;

import com.github.javafaker.Faker;

/**
 * @author thomas.haines@practiceinsight.io
 */
public class FakerUtil {

  private final Faker faker;

  public FakerUtil(Faker faker) {
    this.faker = faker;
  }

  public FakerUtil() {
    this(new Faker());
  }

  public Faker faker() {
    return faker;
  }

  public String getRandom(String prefix) {
    return faker.numerify(prefix = "#######");
  }

}
