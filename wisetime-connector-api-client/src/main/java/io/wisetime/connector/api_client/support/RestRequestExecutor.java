/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.wisetime.connector.api_client.EndpointPath;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

/**
 * WiseTime request executor.
 */
@SuppressWarnings("WeakerAccess")
public class RestRequestExecutor {

  private static final String BASE_URL = "https://wisetime.com/connect/api";
  private final ObjectMapper mapper;
  private final URI apiBaseUrl;
  private final String apiKey;

  public RestRequestExecutor(String apiKey) {
    this(apiKey, Optional.ofNullable(System.getenv("API_BASE_URL")).orElse(BASE_URL));
  }

  public RestRequestExecutor(String apiKey, String apiBaseUrl) {
    this.apiKey = apiKey;
    this.mapper = TolerantObjectMapper.create();
    // if `API_BASE_URL` is set as system property or environment variable, override with supplied URL
    if (!apiBaseUrl.endsWith("/")) {
      apiBaseUrl = apiBaseUrl + "/";
    }
    this.apiBaseUrl = URI.create(apiBaseUrl);
  }

  public <T> T executeTypedRequest(Class<T> valueType,
                                   EndpointPath endpointPath) throws IOException {
    String responseBody = executeRequest(endpointPath, Map.of());
    return mapper.readValue(responseBody, valueType);
  }

  public <T> T executeTypedRequest(Class<T> valueType,
      EndpointPath endpointPath, PathParams pathParams) throws IOException {
    String responseBody = executeRequest(endpointPath, pathParams, Map.of());
    return mapper.readValue(responseBody, valueType);
  }

  /**
   * Using TypeReference instead of class to be able to return typed list results
   */
  public <T> T executeTypedRequest(TypeReference<T> valueType,
      EndpointPath endpointPath, Map<String, String> queryParams) throws IOException {
    String responseBody = executeRequest(endpointPath, queryParams);
    return mapper.readValue(responseBody, valueType);
  }

  public String executeRequest(EndpointPath endpointPath, Map<String, String> queryParams) throws IOException {
    return executeRequest(endpointPath, PathParams.none(), queryParams);
  }

  public String executeRequest(EndpointPath endpointPath, PathParams pathParams, Map<String, String> queryParams)
      throws IOException {
    ConnectApiRequest request = createRequest(endpointPath, pathParams, queryParams, null);
    return request.execute();
  }

  public <T> T executeTypedBodyRequest(Class<T> valueType,
                                       EndpointPath endpointPath,
                                       Object requestBody) throws IOException {
    return executeTypedBodyRequest(valueType, endpointPath, Map.of(), requestBody);
  }

  public <T> T executeTypedBodyRequest(Class<T> valueType, EndpointPath endpointPath,
      Map<String, String> queryParams, Object requestBody) throws IOException {
    String json = mapper.writeValueAsString(requestBody);
    ConnectApiRequest request = createRequest(endpointPath, PathParams.none(), queryParams, json);
    return mapper.readValue(request.execute(), valueType);
  }

  public <T> T executeTypedBodyRequest(Class<T> valueType, EndpointPath endpointPath,
      PathParams pathParams, Object requestBody) throws IOException {
    String json = mapper.writeValueAsString(requestBody);
    ConnectApiRequest request = createRequest(endpointPath, pathParams, Map.of(), json);
    return mapper.readValue(request.execute(), valueType);
  }

  private ConnectApiRequest createRequest(EndpointPath endpointPath, PathParams pathParams,
      Map<String, String> queryParams, String json) {
    return new ConnectApiRequest(
        endpointPath.getHttpMethod(),
        buildEndpointUri(endpointPath, pathParams, queryParams),
        request -> request.setHeader("x-api-key", apiKey),
        json
    );
  }

  @VisibleForTesting
  URI buildEndpointUri(EndpointPath endpointPath, Map<String, String> queryParams) {
    return buildEndpointUri(endpointPath, PathParams.none(), queryParams);
  }

  @VisibleForTesting
  URI buildEndpointUri(EndpointPath endpointPath, PathParams pathParams, Map<String, String> queryParams) {
    try {
      endpointPath.getRequiredQueryParams().forEach(param -> {
        Preconditions.checkState(queryParams.containsKey(param), "Required query parameter %s is missing", param);
      });
      return new URIBuilder(apiBaseUrl.resolve(resolveActionPath(endpointPath, pathParams)))
          .addParameters(queryParams.entrySet().stream()
              .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList()))
          .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Failed to build endpoint url", e);
    }
  }

  private String resolveActionPath(EndpointPath endpointPath, PathParams pathParams) {
    String actionPath = endpointPath.getActionPath();
    for (Map.Entry<String, String> pathParam : pathParams.getValues().entrySet()) {
      actionPath = actionPath.replace("{" + pathParam.getKey() + "}", pathParam.getValue());
    }
    Preconditions.checkState(!actionPath.contains("{"),
        "Missing path parameter(s) for endpoint %s: %s", endpointPath, actionPath);
    return actionPath;
  }
}
