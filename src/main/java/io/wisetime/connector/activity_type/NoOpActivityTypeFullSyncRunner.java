package io.wisetime.connector.activity_type;

/**
 * @author yehor.lashkul
 */
public class NoOpActivityTypeFullSyncRunner extends ActivityTypeFullSyncRunner {

  public NoOpActivityTypeFullSyncRunner() {
    super(null);
  }

  @Override
  public void run() {
    // no action
  }

  @Override
  public boolean isHealthy() {
    return true;
  }
}
