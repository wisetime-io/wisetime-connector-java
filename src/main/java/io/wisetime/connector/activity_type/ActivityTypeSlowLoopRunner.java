package io.wisetime.connector.activity_type;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper class around the activity type slow sync process, that enforces a singleton runner pattern in the event that the
 * previous upload process has not completed prior to the next scheduled check.
 *
 * @author yehor.lashkul
 */
public class ActivityTypeSlowLoopRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public ActivityTypeSlowLoopRunner(WiseTimeConnector connector) {
    super();
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performActivityTypeUpdateSlowLoop();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }
}
