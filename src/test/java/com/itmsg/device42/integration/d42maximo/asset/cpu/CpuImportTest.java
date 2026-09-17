package com.itmsg.device42.integration.d42maximo.asset.cpu;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CpuImportTest {

    @Test
    void roundsSpeedToTargetScale() {
        assertThat(CpuMapper.roundSpeed(new BigDecimal("2.345")))
                .isEqualByComparingTo("2.35");
        assertThat(CpuMapper.roundSpeed(null)).isNull();
    }

    @Test
    void usesModelWhenDescriptionIsBlank() {
        assertThat(CpuMapper.firstNonBlank(" ", "Xeon Gold"))
                .isEqualTo("Xeon Gold");
        assertThat(CpuMapper.firstNonBlank("CPU 1", "Xeon Gold"))
                .isEqualTo("CPU 1");
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(CpuMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(CpuMapper.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(CpuMapper.defaultUnknown(" Intel ")).isEqualTo("Intel");
    }
}
