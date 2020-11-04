package io.wisetime.connector.activity_type;

/**
 * @author yehor.lashkul
 */
public class NoOpActivityTypeSlowLoopRunner extends ActivityTypeSlowLoopRunner {

  public NoOpActivityTypeSlowLoopRunner() {
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
