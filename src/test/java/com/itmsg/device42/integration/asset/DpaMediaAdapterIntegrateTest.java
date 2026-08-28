package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DpaMediaAdapterIntegrateTest {

    @Test
    void fallsBackThroughDescriptionCandidates() {
        assertThat(DpaMediaAdapterIntegrate.firstNonBlank(" ", null, "GeForce RTX 4090"))
                .isEqualTo("GeForce RTX 4090");
        assertThat(DpaMediaAdapterIntegrate.firstNonBlank("VGA controller", null, "GeForce"))
                .isEqualTo("VGA controller");
        assertThat(DpaMediaAdapterIntegrate.firstNonBlank(null, null, null)).isNull();
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaMediaAdapterIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaMediaAdapterIntegrate.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaMediaAdapterIntegrate.defaultUnknown(" NVIDIA ")).isEqualTo("NVIDIA");
    }
}
