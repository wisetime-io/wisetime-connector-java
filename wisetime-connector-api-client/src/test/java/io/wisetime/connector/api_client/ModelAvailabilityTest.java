/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client;

import io.wisetime.generated.connect.GroupActivityType;
import io.wisetime.generated.connect.TimeGroup;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ModelAvailabilityTest {

  @Test
  void confirmModelAvailableFields() {
    TimeGroup group = new TimeGroup();
    group.setLocalDate(LocalDate.now());
    group.setActivityType(new GroupActivityType());
  }

}
