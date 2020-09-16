package io.wisetime.connector.activity_type;

/**
 * @author yehor.lashkul
 */
public class NoOpActivityTypeRunner extends ActivityTypeRunner {

  public NoOpActivityTypeRunner() {
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
