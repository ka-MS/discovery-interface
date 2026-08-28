package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DpaOsIntegrateTest {

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaOsIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaOsIntegrate.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaOsIntegrate.defaultUnknown(" Canonical ")).isEqualTo("Canonical");
    }
}
