/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.wise_log_aws.cloud;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

/**
 * @author thomas.haines@practiceinsight.io
 */
public class LayoutEngineJson extends LayoutBase<ILoggingEvent> {
  private final ThrowableProxyConverter tpc;
  private final ObjectMapper om;
  private final Pattern lineBreakPattern = Pattern.compile("^(.+?)\r?\n.*");
  private static final String EMPTY = "";
  private String moduleName = null;
  private ObjectNode configMap = null;

  LayoutEngineJson() {
    super();
    tpc = new ThrowableProxyConverter();
    tpc.setOptionList(Collections.singletonList("full"));
    om = new ObjectMapper();
    om.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
  }

  @Override
  public String doLayout(ILoggingEvent event) {
    if (!isStarted()) {
      return CoreConstants.EMPTY_STRING;
    }

    return createSingleLineJson(event);
  }

  private String createSingleLineJson(ILoggingEvent event) {
    ObjectNode rootNode = om.createObjectNode();

    // if module name added, use it
    getModuleName().ifPresent(modName -> rootNode.put("b_mod", modName));

    // if config info available, use it
    getConfigMap().ifPresent(configNode -> rootNode.set("source", configNode));

    rootNode.put("a_level", getSeverity(event));
    rootNode.put("b_mesg", getMessage(event, true));
    rootNode.set("context", createContextNode(event));

    try {
      // inside try block we try to use sort serialization of jackson for consistent output / readability
      Object pojo = om.readValue(rootNode.toString(), Object.class);
      return om.writeValueAsString(pojo) + CoreConstants.LINE_SEPARATOR;
    } catch (IOException e) {
      // any issues use put order serialization
      return rootNode.toString() + CoreConstants.LINE_SEPARATOR;
    }
  }

  private ObjectNode createContextNode(ILoggingEvent event) {
    ObjectNode contextNode = om.createObjectNode();
    contextNode.put("logger", event.getLoggerName());
    contextNode.put("thread", event.getThreadName());
    contextNode.put("message", getMessage(event, true));

    // no truncation message
    contextNode.put("message_full", getMessage(event, false));

    if (event.hasCallerData()) {
      StackTraceElement callerData = event.getCallerData()[0];
      if (callerData != null) {
        contextNode.put("filePath", callerData.getClassName().replace('.', '/') + ".class");
        contextNode.put("lineNumber", callerData.getLineNumber());
        contextNode.put("functionName", callerData.getClassName() + "." + callerData.getMethodName());
      }
    }

    String stackTrace = createStackTrace(event);
    if (isNotEmpty(stackTrace)) {
      contextNode.put("stackAdded", true);
      contextNode.put("stackTrace", stackTrace);
    }

    if (event.getMDCPropertyMap() != null && event.getMDCPropertyMap().size() > 0) {
      contextNode.set("mdcPropertyMap", getMdcEvents(event));
    }
    return contextNode;
  }

  private String createStackTrace(ILoggingEvent event) {


    IThrowableProxy tp = event.getThrowableProxy();
    if (tp != null) {
      try {
        String stackTrace = limitStackTrace(tpc.convert(event));
        if (isNotEmpty(stackTrace)) {
          return stackTrace;
        }
      } catch (Exception e) {
        return "failed to log exception error - " + e.getMessage();
      }
    }

    return "";
  }

  private ObjectNode getMdcEvents(ILoggingEvent event) {
    Map<String, String> propertyMap = event.getMDCPropertyMap();
    ObjectNode md5PropertyNode = om.createObjectNode();
    propertyMap.forEach(md5PropertyNode::put);
    return md5PropertyNode;
  }

  private String limitStackTrace(String stackTrace) {
    if (stackTrace == null) {
      return "";
    }

    int sizeOfTrace = stackTrace.length();
    int maxLengthException = 15000;

    if (sizeOfTrace > maxLengthException) {
      // limit to max length
      return stackTrace.substring(0, maxLengthException) + "...";
    }
    return stackTrace;
  }

  private String getMessage(ILoggingEvent event, boolean onlyIncludeFirstLine) {
    StringBuilder messageB = new StringBuilder();

    if (event.hasCallerData()) {
      StackTraceElement callerData = event.getCallerData()[0];
      if (callerData != null && isNotEmpty(callerData.getClassName())) {
        // prepend function name
        final String className = callerData.getClassName().replaceAll(".+\\.", "");
        messageB.append(
            String.format("%s#%s:%d ", className, callerData.getMethodName(), callerData.getLineNumber())
        );
      }
    }

    final String formattedMessage = event.getFormattedMessage();
    if (isNotEmpty(formattedMessage)) {
      // append message if not null
      messageB.append(formattedMessage);
    } else {
      messageB.append("[null]");
    }

    String message = messageB.toString();
    if (onlyIncludeFirstLine) {
      Matcher matcher = lineBreakPattern.matcher(message);
      if (matcher.find()) {
        return matcher.group(1);
      }
    }
    return message;
  }

  @SuppressWarnings("NeedBraces")
  private String getSeverity(final ILoggingEvent event) {
    Level level = event.getLevel();
    if (level == Level.ALL) return "DEBUG";
    else if (level == Level.TRACE) return "DEBUG";
    else if (level == Level.DEBUG) return "DEBUG";
    else if (level == Level.INFO) return "INFO";
    else if (level == Level.WARN) return "WARNING";
    else if (level == Level.ERROR) return "ERROR";
    else return "DEFAULT";
  }

  private String trimToEmpty(String str) {
    return str == null ? EMPTY : str.trim();
  }

  private boolean isNotEmpty(String cs) {
    return cs != null && cs.length() > 0;
  }

  @Override
  public void start() {
    tpc.start();
    super.start();
  }

  @Override
  public void stop() {
    tpc.stop();
    super.stop();
  }

  void addConfig(ConfigPojo configPojo, Map<String, String> configPropertyMap) {
    this.moduleName = configPojo.getModuleName().orElse("unknown").trim();

    if (!configPropertyMap.isEmpty()) {
      configMap = om.createObjectNode();

      configPropertyMap.forEach((key, value) -> {
        if (key != null && value != null) {
          configMap.put(key.toLowerCase().trim(), value.trim());
        }
      });
    }
  }

  private Optional<ObjectNode> getConfigMap() {
    return Optional.ofNullable(configMap);
  }

  private Optional<String> getModuleName() {
    return Optional.ofNullable(moduleName);
  }

}
