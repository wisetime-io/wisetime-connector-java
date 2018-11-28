/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.eclipse.jetty.util.URIUtil;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.wisetime.connector.api_client.EndpointPath;
import io.wisetime.connector.config.ConnectorConfigKey;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.connector.config.TolerantObjectMapper;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings("WeakerAccess")
public class RestRequestExecutor {

  private static final String BASE_URL = "https://wisetime.io/connect/api";
  private final ObjectMapper mapper;
  private final String apiBaseUrl;
  private final String apiKey;

  public RestRequestExecutor(String apiKey) {
    this(apiKey, RuntimeConfig.findString(ConnectorConfigKey.API_BASE_URL).orElse(BASE_URL));
  }

  public RestRequestExecutor(String apiKey, String apiBaseUrl) {
    this.apiKey = apiKey;
    this.mapper = TolerantObjectMapper.create();
    // if `API_BASE_URL` is set as system property or environment variable, override with supplied URL
    this.apiBaseUrl = apiBaseUrl;
  }

  public <T> T executeTypedRequest(Class<T> valueType,
                                   EndpointPath endpointPath) throws IOException {
    String responseBody = executeRequest(endpointPath, Lists.newArrayList());
    return mapper.readValue(responseBody, valueType);
  }

  public <T> T executeTypedRequest(Class<T> valueType,
                                   EndpointPath endpointPath,
                                   List<NameValuePair> params) throws IOException {
    String responseBody = executeRequest(endpointPath, params);
    return mapper.readValue(responseBody, valueType);
  }

  public String executeRequest(EndpointPath endpointPath, List<NameValuePair> params) throws IOException {
    ConnectApiRequest request = createRequest(endpointPath, params, null);
    return request.execute();
  }

  public <T> T executeTypedBodyRequest(Class<T> valueType,
                                       EndpointPath endpointPath,
                                       Object requestBody) throws IOException {
    return executeTypedBodyRequest(valueType, endpointPath, null, requestBody);
  }

  public <T> T executeTypedBodyRequest(Class<T> valueType,
                                       EndpointPath endpointPath,
                                       List<NameValuePair> params,
                                       Object requestBody) throws IOException {
    String json = mapper.writeValueAsString(requestBody);
    ConnectApiRequest request = createRequest(endpointPath, params, json);
    return mapper.readValue(request.execute(), valueType);
  }

  private ConnectApiRequest createRequest(EndpointPath endpointPath, List<NameValuePair> params, String json) {
    String endpointActionUri = mergeNamedParams(endpointPath.getActionPath(), params);
    return new ConnectApiRequest(
        endpointPath.getHttpMethod(),
        buildEndpointUri(endpointActionUri),
        request -> request.setHeader("x-api-key", apiKey),
        json
    );
  }

  private String buildEndpointUri(String endpointPath) {
    checkArgument(StringUtils.isNotEmpty(endpointPath), "rest request endpoint path is required.");
    return apiBaseUrl + endpointPath;
  }

  @VisibleForTesting
  String mergeNamedParams(String actionPath, List<NameValuePair> namedParamList) {
    if (namedParamList == null || namedParamList.isEmpty()) {
      return actionPath;
    }

    String mergedTemplate = actionPath;
    for (NameValuePair keyValuePair : namedParamList) {
      String matchStr = String.format("(:%s)(/|$)", keyValuePair.getName());
      Pattern pattern = Pattern.compile(matchStr, Pattern.CASE_INSENSITIVE);
      Matcher match = pattern.matcher(mergedTemplate);
      if (!match.find()) {
        throw new RuntimeException(String.format("No named variable '%s' found in path '%s'",
            keyValuePair.getName(), mergedTemplate));
      }
      int startIndex = match.start(1);
      int endIndex = match.end(1);

      // replace matched group with value
      mergedTemplate = new StringBuilder(mergedTemplate)
          .replace(startIndex, endIndex, URIUtil.encodePath(keyValuePair.getValue()))
          .toString();
    }

    return mergedTemplate;
  }
}
