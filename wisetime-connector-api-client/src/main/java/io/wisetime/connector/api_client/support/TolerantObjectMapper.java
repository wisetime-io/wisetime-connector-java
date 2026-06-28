/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A static singleton of a tolerant object mapper concerning additional of new fields and the like.
 *
 * @author thomas.haines
 */
public class TolerantObjectMapper {

  public static ObjectMapper create() {
    return new ObjectMapper()
        .enable(Feature.ALLOW_COMMENTS)
        .enable(Feature.IGNORE_UNDEFINED)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule())
        .setDateFormat(new StdDateFormat());
  }
}
