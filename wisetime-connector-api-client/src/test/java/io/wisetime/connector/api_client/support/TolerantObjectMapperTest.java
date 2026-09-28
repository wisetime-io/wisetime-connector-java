/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author thomas.haines
 */
class TolerantObjectMapperTest {

  private static final Logger log = LoggerFactory.getLogger(TolerantObjectMapperTest.class);

  private final ObjectMapper objectMapper = TolerantObjectMapper.create();

  @Test
  void checkDate() throws JsonProcessingException {
    DateTest dateTest = new DateTest()
        .setGroupTs(ZonedDateTime.now().getOffset())
        .setGroupDate(LocalDate.now());
    String serializedObject = objectMapper.writeValueAsString(dateTest);
    assertThat(objectMapper.readValue(serializedObject, DateTest.class))
        .as("java8 datetime should be mapped successfully")
        .isEqualTo(dateTest);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    ZonedDateTime parsed = ZonedDateTime.parse("20010203040506000", formatter);
    String result = objectMapper.writeValueAsString(parsed);
    assertThat(result)
        .isEqualTo("\"2001-02-03T04:05:06Z\"");
  }

  @Data
  @Accessors(chain = true)
  static class DateTest {

    LocalDate groupDate;
    ZoneOffset groupTs;
  }


  @Test
  void create() {
    assertThat(runTest(new ObjectMapper())).isNotPresent();
    assertThat(runTest(objectMapper)).contains("success");
  }

  private Optional<String> runTest(ObjectMapper objectMapper) {
    final String json = " {  \"noSuchProp\" : true, \"Property\" : \"success\"   } ";

    try {
      return Optional.of(objectMapper.readValue(json, MapTest.class).getProperty());
    } catch (Exception e) {
      log.debug(e.getMessage());
      return Optional.empty();
    }
  }

  static class MapTest {

    private String property;

    String getProperty() {
      return property;
    }

    MapTest setProperty(String property) {
      this.property = property;
      return this;
    }
  }
}
