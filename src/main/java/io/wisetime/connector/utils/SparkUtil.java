/*
 * Copyright (c) 2018 Practice Insight Pty Ltd. All Rights Reserved.
 */

package io.wisetime.connector.utils;

import com.google.common.base.Preconditions;

import spark.ExceptionHandler;
import spark.ExceptionHandlerImpl;
import spark.ExceptionMapper;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * @author yehor.lashkul
 */
public class SparkUtil {

  public static RouteWithExceptionHandlers withExceptionHandlers(Route route) {
    return new RouteWithExceptionHandlers(route);
  }

  /**
   * A simple {@link Route} wrapper which gives ability to add exception handlers to the route. This class may be useful when
   * using Spark in servlet mode and until https://github.com/perwendel/spark/issues/1062 is fixed
   */
  public static class RouteWithExceptionHandlers implements Route {
    private final Route route;
    private final ExceptionMapper exceptionMapper;

    private RouteWithExceptionHandlers(Route route) {
      Preconditions.checkNotNull(route, "Route is required");
      this.route = route;
      this.exceptionMapper = new ExceptionMapper();
    }

    public <T extends Exception> RouteWithExceptionHandlers exception(Class<T> exceptionClass,
                                                                      ExceptionHandler<? super T> handler) {
      Preconditions.checkNotNull(handler, "Exception handler can't be null");
      exceptionMapper.map(exceptionClass, wrapHandler(exceptionClass, handler));
      return this;
    }

    private static <T extends Exception> ExceptionHandlerImpl<T> wrapHandler(Class<T> exceptionClass,
                                                                             ExceptionHandler<? super T> handler) {
      return new ExceptionHandlerImpl<T>(exceptionClass) {
        @Override
        public void handle(T exception, Request request, Response response) {
          handler.handle(exception, request, response);
        }
      };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object handle(Request request, Response response) throws Exception {
      try {
        return route.handle(request, response);
      } catch (Exception e) {
        ExceptionHandlerImpl handler = exceptionMapper.getHandler(e);
        if (handler != null) {
          handler.handle(e, request, response);
          return response.body();
        } else {
          throw e;
        }
      }
    }
  }
}
