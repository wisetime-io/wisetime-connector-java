package io.wisetime.connector.activity_type;

import io.wisetime.connector.WiseTimeConnector;
import io.wisetime.connector.utils.BaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yehor.lashkul
 */
public class ActivityTypeRunner extends BaseRunner {

  private final WiseTimeConnector connector;

  public ActivityTypeRunner(WiseTimeConnector connector) {
    super();
    this.connector = connector;
  }

  @Override
  protected void performAction() {
    connector.performActivityTypeUpdate();
  }

  @Override
  protected Logger getLogger() {
    return LoggerFactory.getLogger(connector.getClass());
  }
}
