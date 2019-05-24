/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author thomas.haines
 */
@SuppressWarnings("Duplicates")
public class PropertyFileUtil {
  public static Map<String, String> addUserPropertyFile(String filePath) {
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
}
