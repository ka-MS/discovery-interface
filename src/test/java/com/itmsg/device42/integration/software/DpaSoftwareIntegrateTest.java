package com.itmsg.device42.integration.software;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DpaSoftwareIntegrateTest {

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaSoftwareIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaSoftwareIntegrate.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaSoftwareIntegrate.defaultUnknown(" Device42 ")).isEqualTo("Device42");
    }
}
