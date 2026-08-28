package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DpaCpuIntegrateTest {

    @Test
    void roundsSpeedToTargetScale() {
        assertThat(DpaCpuIntegrate.roundSpeed(new BigDecimal("2.345")))
                .isEqualByComparingTo("2.35");
        assertThat(DpaCpuIntegrate.roundSpeed(null)).isNull();
    }

    @Test
    void usesModelWhenDescriptionIsBlank() {
        assertThat(DpaCpuIntegrate.firstNonBlank(" ", "Xeon Gold"))
                .isEqualTo("Xeon Gold");
        assertThat(DpaCpuIntegrate.firstNonBlank("CPU 1", "Xeon Gold"))
                .isEqualTo("CPU 1");
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaCpuIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaCpuIntegrate.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaCpuIntegrate.defaultUnknown(" Intel ")).isEqualTo("Intel");
    }
}
