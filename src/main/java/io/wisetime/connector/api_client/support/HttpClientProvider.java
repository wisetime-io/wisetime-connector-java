/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.api_client.support;

import javax.net.ssl.SSLContext;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 *  Most of class is adapted from org.apache.http.client.fluent.{@link org.apache.http.client.fluent.Executor}
 *
 *  The changes are to:
 *  1. Enable retry handler (up to 2 times on idempotent method types)
 *  2. Don't reuse connections (no keep alive)
 * </pre>
 */
class HttpClientProvider {

  private static final Logger log = LoggerFactory.getLogger(HttpClientProvider.class);
  private static final PoolingHttpClientConnectionManager CONNMGR;
  private static final HttpClient CLIENT;

  static HttpClient getHttpClient() {
    return CLIENT;
  }

  /**
   * MAX_CONNECTIONS_PER_ROUTE is the maximum number of connections to a particular host.
   */
  private static final int MAX_CONNECTIONS_PER_ROUTE = 100;

  /**
   * MAX_CONNECTIONS is the maximum total number of connections in the pool
   */
  private static final int MAX_CONNECTIONS = 200;

  static {
    LayeredConnectionSocketFactory ssl = null;
    try {
      ssl = SSLConnectionSocketFactory.getSystemSocketFactory();
    } catch (final SSLInitializationException ex) {
      final SSLContext sslcontext;
      try {
        sslcontext = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
        sslcontext.init(null, null, null);
        ssl = new SSLConnectionSocketFactory(sslcontext);
      } catch (Exception except) {
        log.error("Failed to initialise SSL context", except);
      }
    }

    final Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", ssl != null ? ssl : SSLConnectionSocketFactory.getSocketFactory())
        .build();
    CONNMGR = new PoolingHttpClientConnectionManager(sfr);
    CONNMGR.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
    CONNMGR.setMaxTotal(MAX_CONNECTIONS);
    CONNMGR.setValidateAfterInactivity(1000);
    CLIENT = HttpClientBuilder.create()
        .setConnectionManager(CONNMGR)
        // try 2 times on idempotent methods
        .setRetryHandler(new StandardHttpRequestRetryHandler(2, false))
        .setConnectionReuseStrategy(new NoConnectionReuseStrategy())
        .build();
  }
}
