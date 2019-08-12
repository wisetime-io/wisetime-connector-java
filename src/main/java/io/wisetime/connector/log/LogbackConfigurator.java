package io.wisetime.connector.log;

import com.google.common.annotations.VisibleForTesting;

import com.google.common.base.Preconditions;
import io.wisetime.generated.connect.ManagedConfigResponse;
import java.util.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.wisetime.connector.config.RuntimeConfig;
import lombok.extern.slf4j.Slf4j;
import spark.utils.StringUtils;

/**
 * @author thomas.haines
 */
@Slf4j
public class LogbackConfigurator {

  @VisibleForTesting
  static final String SERVICE_ID_DELIMITER = ":";

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
        Pair<String, String> serviceCredentials = getServiceCredentials(config.getServiceId());

        localAdapterCW = LocalAdapterCW.builder()
            .accessKey(serviceCredentials.getLeft())
            .secretKey(serviceCredentials.getRight())
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

  @VisibleForTesting
  static Pair<String, String> getServiceCredentials(String serviceIdBase64) {
    Preconditions.checkArgument(StringUtils.isNotBlank(serviceIdBase64));

    String serviceId = new String(Base64.getDecoder().decode(serviceIdBase64.getBytes()));
    String[] serviceIdArray = serviceId.split(SERVICE_ID_DELIMITER);
    return Pair.of(serviceIdArray[0], serviceIdArray[1]);
  }
}
