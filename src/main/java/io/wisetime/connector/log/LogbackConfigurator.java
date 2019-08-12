package io.wisetime.connector.log;

import com.google.common.annotations.VisibleForTesting;

import io.wisetime.generated.connect.ManagedConfigResponse;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.wisetime.connector.config.RuntimeConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * @author thomas.haines
 */
@Slf4j
public class LogbackConfigurator {

  public static void configureBaseLogging(ManagedConfigResponse config) {
    setLogLevel();

    final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    if (rootLogger.getAppender(LocalAdapterCW.class.getSimpleName()) == null) {
      Optional<Appender<ILoggingEvent>> localAppender = createLocalAdapter(config);
      localAppender.ifPresent(rootLogger::addAppender);
    }
  }

  static void setLogLevel() {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    RuntimeConfig.getString(() -> "LOG_LEVEL").ifPresent(levelStr -> {
      // follow runtime specified log level for root logger
      Level level = Level.toLevel(levelStr, Level.INFO);
      rootLogger.setLevel(level);
    });
  }

  @VisibleForTesting
  static Optional<Appender<ILoggingEvent>> createLocalAdapter(ManagedConfigResponse config) {
    try {
      final LocalAdapterCW localAdapterCW;

      if (config != null) {
        localAdapterCW = LocalAdapterCW.builder()
            .accessKey(config.getServiceId())
            .secretKey(config.getServiceKey())
            .regionName(config.getRegionName())
            .logGroupName(config.getGroupName())
            .build();

      } else {
        localAdapterCW = LocalAdapterCW.builder().build();
      }

      AppenderPipe localConfigAppender = new AppenderPipe(localAdapterCW);
      localConfigAppender.setName(LocalAdapterCW.class.getSimpleName());

      // deny all events with a level below WARN, except for io.wisetime.* packages
      ThresholdFilterWithExclusion thresholdFilter = new ThresholdFilterWithExclusion();
      thresholdFilter.setExcludedLogPrefixList("io.wisetime.");
      thresholdFilter.setLevel(Level.WARN.toString());
      localConfigAppender.addFilter(thresholdFilter);
      localConfigAppender.start();
      return Optional.of(localConfigAppender);

    } catch (Throwable throwable) {
      log.warn(throwable.getMessage(), throwable);
      return Optional.empty();
    }
  }
}
