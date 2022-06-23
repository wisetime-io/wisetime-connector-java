/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import static io.wisetime.connector.config.ConnectorConfigKey.CONNECTOR_PROPERTIES_FILE;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides runtime configuration for WiseTime connectors
 *
 * @author thomas.haines
 * @author shane.xie
 * @author vadym.sukolenvo
 * @author yehor.lashkul
 */
@SuppressWarnings("WeakerAccess")
public class RuntimeConfig {

  // TODO(TH) discuss with Yehor
  // Don't remove this logger!!!
  // Because our logback configurations depends on RuntimeConfig instance, first logger creation
  // shouldn't happen during RuntimeConfig construction. To be sure this doesn't happen we initialize logger here
  // and use lazy RuntimeConfig instance creation.
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(RuntimeConfig.class);

  private final CompositeConfiguration config;
  private final MapConfiguration inMemoryConfig;

  private RuntimeConfig() {
    config = new CompositeConfiguration();
    // first tier system properties
    config.addConfiguration(new CaseInsensitiveSystemConfiguration());

    // second tier environment variables
    inMemoryConfig = new MapConfiguration(new CaseInsensitiveMap<String, Object>(System.getenv()));
    config.addConfiguration(inMemoryConfig);

    String connectorPropertyFilePath = config.getString(CONNECTOR_PROPERTIES_FILE.getConfigKey());
    if (StringUtils.isNotBlank(connectorPropertyFilePath)) {

      // add property file if provided (also passed to logback for additional logging context)
      config.addConfiguration(
          new MapConfiguration(new CaseInsensitiveMap<>(addUserPropertyFile(connectorPropertyFilePath))));
    }

    // parse json file if specified and add to config
    JsonMapUtil.parseJsonFileToMap(config.getString("json_config", ""))
        .ifPresent(config::addConfiguration);

  }

  /**
   * Get String config value for key
   *
   * @param configKey the key for which to get the value
   * @return the String value, or an empty optional if there is no value configured for the key
   */
  public static Optional<String> getString(RuntimeConfigKey configKey) {
    return getInstance().getString(configKey.getConfigKey());
  }

  private Optional<String> getString(String key) {
    return Optional.ofNullable(StringUtils.trimToNull(config.getString(key)));
  }

  /**
   * Get Integer config value for key
   *
   * @param configKey the key for which to get the value
   * @return the Integer value, or an empty optional if there is no value configured for the key
   */
  public static Optional<Integer> getInt(RuntimeConfigKey configKey) {
    return getInstance().getInt(configKey.getConfigKey());
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

  private Optional<Boolean> getBoolean(String key) {
    final String boolAsString = StringUtils.trimToEmpty(config.getString(key));
    if (!boolAsString.isEmpty()) {
      try {
        return Optional.of(Boolean.valueOf(boolAsString));
      } catch (Throwable t) {
        System.err.println(String.format("Config value '%s' is an invalid boolean, ignoring", boolAsString));
      }
    }
    // not provided or could not be parsed to boolean
    return Optional.empty();
  }

  /**
   * Get Boolean config value for key
   *
   * @param configKey the key for which to get the value
   * @return the Boolean value, or an empty optional if there is no value configured for the key
   */
  public static Optional<Boolean> getBoolean(RuntimeConfigKey configKey) {
    return getInstance().getBoolean(configKey.getConfigKey());
  }

  /**
   * This property can removed by {@link #clearProperty(RuntimeConfigKey)}.
   * All properties set by this method will not survive though {@link #rebuild()}.
   * In case same key provided from multiple sources - this method overcome file configuration,
   * but not system properties.
   */
  public static void setProperty(RuntimeConfigKey configKey, String value) {
    getInstance().inMemoryConfig.setProperty(configKey.getConfigKey(), value);
  }

  /**
   * This method will clean up only configuration set by {@link #setProperty(RuntimeConfigKey, String)}.
   * This method will not effect properties provided from environment config or file config.
   */
  public static void clearProperty(RuntimeConfigKey configKey) {
    getInstance().inMemoryConfig.clearProperty(configKey.getConfigKey());
  }

  @VisibleForTesting
  public static void rebuild() {
    LazyInstanceHolder.INSTANCE = new RuntimeConfig();
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

  // Use lazy instance initialization to prevent screwing logger configuration up.
  // More details see in the logger creation comment above
  private static RuntimeConfig getInstance() {
    return LazyInstanceHolder.INSTANCE;
  }

  private static class LazyInstanceHolder {
    public static RuntimeConfig INSTANCE = new RuntimeConfig();
  }

  static class CaseInsensitiveSystemConfiguration extends SystemConfiguration {
    @SuppressWarnings("unchecked")
    CaseInsensitiveSystemConfiguration() {
      map = new CaseInsensitiveMap(map);
    }
  }
}
