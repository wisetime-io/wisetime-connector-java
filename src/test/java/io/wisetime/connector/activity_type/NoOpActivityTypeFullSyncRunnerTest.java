package io.wisetime.connector.activity_type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class NoOpActivityTypeFullSyncRunnerTest {

  private ActivityTypeFullSyncRunner activityTypeFullSyncRunner;

  @BeforeEach
  void setup() {
    activityTypeFullSyncRunner = new NoOpActivityTypeFullSyncRunner();
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = activityTypeFullSyncRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> activityTypeFullSyncRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(activityTypeFullSyncRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(activityTypeFullSyncRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    activityTypeFullSyncRunner.lastSuccessfulRun = new DateTime().minusYears(1);

    assertThat(activityTypeFullSyncRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
