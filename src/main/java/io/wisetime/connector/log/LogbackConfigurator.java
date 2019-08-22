package io.wisetime.connector.log;

import static io.wisetime.connector.log.LoggerNames.HEART_BEAT_LOGGER_NAME;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.annotations.VisibleForTesting;
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

  public static void configureBaseLogging(ManagedConfigResponse config) {
    setLogLevel();

    final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    if (rootLogger.getAppender(LocalAdapterCW.class.getSimpleName()) == null) {
      Optional<Appender<ILoggingEvent>> localAppender = createLocalAdapter();
      localAppender.ifPresent(rootLogger::addAppender);
      localAppender.ifPresent(LogbackConfigurator::configBaseHeartBeatLogging);
    }
    refreshCredentials(config);
  }

  @VisibleForTesting
  static void configBaseHeartBeatLogging(Appender<ILoggingEvent> appender) {
    final Logger heartBeatLogger = (Logger) LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName());
    if (heartBeatLogger.getAppender(LocalAdapterCW.class.getSimpleName()) == null) {
      heartBeatLogger.addAppender(appender);
      // By default, a log message will be displayed by the logger which writes it, as well as the ancestor loggers.
      // Since root is the ancestor of all loggers, all messages will also be displayed by the root logger. To disable
      // this behavior set additive=false.
      heartBeatLogger.setAdditive(false);
    }
  }

  static void setLogLevel() {
    final Logger rootLogger = (Logger) LoggerFactory.getLogger("root");
    final Logger healthLogger = (Logger) LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName());

    RuntimeConfig.getString(() -> "LOG_LEVEL").ifPresent(levelStr -> {
      // follow runtime specified log level for root logger
      Level level = Level.toLevel(levelStr, Level.INFO);
      rootLogger.setLevel(level);
      healthLogger.setLevel(level);
    });
  }

  @VisibleForTesting
  static Optional<Appender<ILoggingEvent>> createLocalAdapter() {
    try {
      AppenderPipe localConfigAppender = new AppenderPipe(new LocalAdapterCW());
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

  /**
   * Refresh the temporary credentials for the AWS logger.
   */
  private static void refreshCredentials(ManagedConfigResponse config) {
    refreshLocalAdapterCWCredentials(
        (Logger) LoggerFactory.getLogger(HEART_BEAT_LOGGER_NAME.getName()), config);

    refreshLocalAdapterCWCredentials(
        (Logger) LoggerFactory.getLogger("root"), config);
  }

  @VisibleForTesting
  static void refreshLocalAdapterCWCredentials(Logger logger, ManagedConfigResponse config) {
    final Appender<ILoggingEvent> appender = logger.getAppender(LocalAdapterCW.class.getSimpleName());
    if (appender instanceof AppenderPipe) {
      AppenderPipe appenderPipe = (AppenderPipe) appender;
      if (appenderPipe.getBridge() instanceof LocalAdapterCW) {
        ((LocalAdapterCW) ((AppenderPipe) appender).getBridge()).init(config);
      }
    }
  }
}
