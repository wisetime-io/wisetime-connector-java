package io.wisetime.connector.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author yehor.lashkul
 */
class NoOpTagRunnerTest {

  private TagRunner tagRunner;

  @BeforeEach
  void setup() {
    tagRunner = new NoOpTagRunner();
  }

  @Test
  void testRun() throws Exception {
    DateTime startRun = tagRunner.lastSuccessfulRun;
    Thread.sleep(1);

    assertThatCode(() -> tagRunner.run())
        .as("run should do nothing")
        .doesNotThrowAnyException();

    assertThat(tagRunner.lastSuccessfulRun)
        .as("expect last success shouldn't be updated")
        .isEqualTo(startRun);
  }

  @Test
  void testIsHealthy() {
    assertThat(tagRunner.isHealthy())
        .as("always healthy")
        .isTrue();

    tagRunner.lastSuccessfulRun = new DateTime().minusYears(1);

    assertThat(tagRunner.isHealthy())
        .as("always healthy")
        .isTrue();
  }

}
