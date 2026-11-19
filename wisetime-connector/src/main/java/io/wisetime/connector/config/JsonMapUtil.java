/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.lang3.StringUtils;

/**
 * @author thomas.haines
 */
@Slf4j
public class JsonMapUtil {

  /**
   * Parses root children of a json String into a Map.
   */
  static Map<String, String> parseRootJsonChildrenToMap(String jsonContent) {
    Map<String, String> jsonMap = new HashMap<>();
    Json.parse(jsonContent).asObject().iterator()
        .forEachRemaining(member -> jsonMap.put(member.getName(), member.getValue().asString()));
    return jsonMap;
  }

  /**
   * @param jsonConfigVal assumes json file path (file:// and https:// may be added later if use case presents)
   * @return MapConfiguration containing the keys/values inside top-level of json object.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Optional<MapConfiguration> parseJsonFileToMap(String jsonConfigVal) {
    try {
      if (StringUtils.isEmpty(jsonConfigVal)) {
        log.debug("No json file path supplied");
        return Optional.empty();
      }

      final JsonValue jsonValue;
      if (jsonConfigVal.startsWith("file:") || jsonConfigVal.matches("^http(s)?:.+")) {
        final ReadableByteChannel readChannel = Channels.newChannel(new URL(jsonConfigVal).openStream());
        jsonValue = Json.parse(Channels.newReader(readChannel, "utf-8"));

      } else {
        jsonValue = Json.parse(Files.newBufferedReader(Paths.get(jsonConfigVal)));
      }

      Map<String, String> jsonMap = parseRootJsonChildrenToMap(jsonValue.asObject().toString());
      MapConfiguration mapConfig = new MapConfiguration(new CaseInsensitiveMap(jsonMap));
      return Optional.of(mapConfig);
    } catch (Throwable t) {
      log.warn(t.getMessage(), t);
      // json_config only used for integration testing, warn only / consume exception
      return Optional.empty();
    }
  }
}
