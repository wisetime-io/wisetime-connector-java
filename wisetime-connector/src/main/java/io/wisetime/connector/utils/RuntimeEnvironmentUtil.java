/*
 * Copyright (c) 2019 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import io.wisetime.connector.controller.ConnectorControllerImpl;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dusan.grubjesic@staff.wisetime.com
 */
public class RuntimeEnvironmentUtil {

  public static Map<String, String> getEnvProperties() {
    Map<String, String> env = new HashMap<>();
    env.put("java_vm_specific_version", System.getProperty("java.vm.specification.version"));
    env.put("java_vm_version", System.getProperty("java.vm.version"));
    env.put("java_vm_name", System.getProperty("java.vm.name"));
    env.put("client_os", System.getProperty("os.name"));
    return env;
  }

  public static String getLibraryImplVersion() {
    return ConnectorControllerImpl.class.getPackage().getImplementationVersion();
  }
}