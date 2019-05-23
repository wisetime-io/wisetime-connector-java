package io.wisetime.connector.managed_config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author thomas.haines
 */
class DiagnosticWriterTest {

  @BeforeEach
  void setUp() {
  }

  @Test
  void keepMsgStatic() {
    assertThat(DiagnosticWriter.HEARTBEAT_MSG)
        .as("text is static in use as inferred metric")
        .isEqualTo("WISE_CONNECT_HEARTBEAT");
  }
}
