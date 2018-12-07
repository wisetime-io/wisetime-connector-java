/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static io.wisetime.connector.config.ConnectorConfigKey.CONNECTOR_PROPERTIES_FILE;

/**
 * Provides runtime configuration for WiseTime connectors
 *
 * @author thomas.haines@practiceinsight.io
 */
@SuppressWarnings("WeakerAccess")
public class RuntimeConfig {

  private final CompositeConfiguration config;
  private static RuntimeConfig INSTANCE = new RuntimeConfig();

  private RuntimeConfig() {
    config = new CompositeConfiguration();
    // first tier system properties
    config.addConfiguration(new CaseInsensitiveSystemConfiguration());

    // second tier environment variables
    config.addConfiguration(new MapConfiguration(new CaseInsensitiveMap<String, Object>(System.getenv())));

    String connectorPropertyFilePath = config.getString(CONNECTOR_PROPERTIES_FILE.getConfigKey());
    if (StringUtils.isNotBlank(connectorPropertyFilePath)) {
      // add property file if provided (also passed to logback for additional logging context)
      config.addConfiguration(
          new MapConfiguration(new CaseInsensitiveMap<>(addUserPropertyFile(connectorPropertyFilePath))));
    }
  }

  private Optional<String> getString(String key) {
    return Optional.ofNullable(StringUtils.trimToNull(config.getString(key)));
  }

  private Optional<Integer> getInt(String key) {
    final String intAsString = StringUtils.trimToEmpty(config.getString(key));
    if (!intAsString.isEmpty()) {
      try {
        return Optional.of(Integer.valueOf(intAsString));
      } catch (Throwable t) {
        System.err.println(String.format("Config value '%s' is an invalid integer, ignoring", intAsString));
      }
    }
    // not provided or could not be parsed to int
    return Optional.empty();
  }

  /**
   * Get Integer config value for key
   *
   * @param configKey the key for which to get the value
   * @return the Integer value, or an empty optional if there is no value configured for the key
   */
  public static Optional<Integer> getInt(RuntimeConfigKey configKey) {
    return INSTANCE.getInt(configKey.getConfigKey());
  }

  /**
   * Get String config value for key
   *
   * @param configKey the key for which to get the value
   * @return the String value, or an empty optional if there is no value configured for the key
   */
  public static Optional<String> getString(RuntimeConfigKey configKey) {
    return INSTANCE.getString(configKey.getConfigKey());
  }

  public static void setProperty(RuntimeConfigKey configKey, String value) {
    System.setProperty(configKey.getConfigKey(), value);
    rebuild();
  }

  public static void clearProperty(RuntimeConfigKey configKey) {
    System.clearProperty(configKey.getConfigKey());
    rebuild();
  }

  @VisibleForTesting
  public static void rebuild() {
    INSTANCE = new RuntimeConfig();
  }

  static Map<String, String> addUserPropertyFile(String filePath) {
    final Map<String, String> configMap = new HashMap<>();
    try {
      File propertyFile = new File(filePath);
      if (propertyFile.exists()) {
        try (InputStream input = new FileInputStream(propertyFile)) {
          final Properties properties = new Properties();
          properties.load(input);
          properties.stringPropertyNames().forEach(propertyKey -> {
            configMap.put(propertyKey, properties.getProperty(propertyKey));
          });
        }
      } else {
        System.err.println("propertiesFilePath not found" + filePath);
      }
    } catch (Throwable t) {
      System.err.println("propertiesFilePath failed to initialise, msg=" + t.getMessage());
    }
    return configMap;
  }


  static class CaseInsensitiveSystemConfiguration extends SystemConfiguration {
    @SuppressWarnings("unchecked")
    CaseInsensitiveSystemConfiguration() {
      map = new CaseInsensitiveMap(map);
    }
  }
}
