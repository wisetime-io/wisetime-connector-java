package io.wisetime.connector.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.wisetime.connector.config.RuntimeConfig;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

/**
 * @author thomas.haines
 */
@Slf4j
public class LogbackConfigurator {

  static LocalAdapterCW localAdapterCW;

  public static void configureBaseLogging(ManagedConfigResponse config) {
    setLogLevel();

    final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    if (rootLogger.getAppender(LocalAdapterCW.class.getSimpleName()) == null) {
      Optional<Appender<ILoggingEvent>> localAppender = createLocalAdapter(config);
      localAppender.ifPresent(rootLogger::addAppender);

    } else {
      refreshLocalAdapterCW(config);
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
      localAdapterCW = new LocalAdapterCW();
      refreshLocalAdapterCW(config);

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

  static void refreshLocalAdapterCW(ManagedConfigResponse config) {
    Preconditions.checkArgument(localAdapterCW != null, "localAdapterCW has not yet been instantiated");

    // temporary credentials for the AWS logger
    localAdapterCW.init(config);
  }
}
