package io.wisetime.connector.activity_type;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yehor.lashkul
 */
public class ActivityTypeFullSyncRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public ActivityTypeFullSyncRunner(WiseTimeConnector connector) {
    super();
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performActivityTypeFullSync();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }

  @Override
  protected int getMaxMinsSinceSuccess() {
    return (int) TimeUnit.DAYS.toMinutes(2);
  }
}
