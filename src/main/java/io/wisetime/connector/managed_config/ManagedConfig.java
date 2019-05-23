package io.wisetime.connector.managed_config;

import com.google.common.collect.Lists;

import org.apache.commons.configuration2.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * @author thomas.haines
 */
@Slf4j
public class ManagedConfig {

  private static final ManagedConfig INSTANCE = new ManagedConfig();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final List<ManagedConfigListener> configListenerList = Lists.newArrayList();
  private final AtomicReference<Configuration> currentConfig = new AtomicReference<>(null);

  private ManagedConfig() {
    // do not instantiate / utility static singleton library
  }

  public static ManagedConfig instance() {
    return INSTANCE;
  }

  /**
   * This method is package private on the expectation that the `registerConfigListener` method will be used to access the
   * config and to response to changes in the managed config property list.
   */
  Optional<Configuration> getConfig() {
    if (running.compareAndSet(false, true)) {

    }
    return Optional.ofNullable(currentConfig.get());
  }

  /**
   * Use instance() method to access deRegister method.
   */
  public void deRegister(ManagedConfigListener managedConfigListener) {
    boolean removed = configListenerList.remove(managedConfigListener);
    if (!removed) {
      log.warn("listener was not found in list of registered listeners");
      // skip if empty list as size of list did not reduce
      return;
    }

    if (configListenerList.isEmpty()) {
      log.debug("Last config listener removed.");
    }
  }

  /**
   * Use instance() method to access registration method.
   *
   * @return Current managed config.
   */
  public Optional<Configuration> registerConfigListener(ManagedConfigListener managedConfigListener) {
    if (configListenerList
        .stream()
        .noneMatch(config -> config == managedConfigListener)) {
      configListenerList.add(managedConfigListener);

    } else {
      log.warn("Listener is already active - registration step skipped");
    }
    return getConfig();
  }

  interface ManagedConfigListener {
    void managedConfigUpdate(@Nullable Configuration managedConfig);
  }
}
