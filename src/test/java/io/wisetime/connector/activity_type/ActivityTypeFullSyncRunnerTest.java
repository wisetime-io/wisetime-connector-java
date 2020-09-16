package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.wisetime.connector.WiseTimeConnector;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class ActivityTypeFullSyncRunnerTest {

  private WiseTimeConnector connector;
  private ActivityTypeFullSyncRunner activityTypeFullSyncRunner;

  @BeforeEach
  void setup() {
    connector = mock(WiseTimeConnector.class);
    activityTypeFullSyncRunner = new ActivityTypeFullSyncRunner(connector);
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = activityTypeFullSyncRunner.lastSuccessfulRun;
    Thread.sleep(1);
    activityTypeFullSyncRunner.run();

    assertThat(activityTypeFullSyncRunner.lastSuccessfulRun)
        .as("expect last success was updated")
        .isGreaterThan(startRun);

    verify(connector, times(1)).performActivityTypeFullSync();
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeFullSyncRunner.isHealthy())
        .as("last successful run is just about now - expecting to return true")
        .isTrue();
  }

  @Test
  void testIsUnHealthy() {
    activityTypeFullSyncRunner.lastSuccessfulRun = new DateTime().minusYears(1);
    assertThat(activityTypeFullSyncRunner.isHealthy())
        .as("last successful run was long time ago - expecting false")
        .isFalse();
  }
}
