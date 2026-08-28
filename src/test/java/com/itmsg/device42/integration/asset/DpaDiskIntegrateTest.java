package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DpaDiskIntegrateTest {

    @Test
    void roundsTotalSpaceToTargetScale() {
        assertThat(DpaDiskIntegrate.roundTotalSpace(new BigDecimal("1024.126")))
                .isEqualByComparingTo("1024.13");
        assertThat(DpaDiskIntegrate.roundTotalSpace(null)).isNull();
    }

    @Test
    void usesModelWhenDescriptionIsBlank() {
        assertThat(DpaDiskIntegrate.firstNonBlank(" ", "Samsung SSD 870 QVO 1TB"))
                .isEqualTo("Samsung SSD 870 QVO 1TB");
        assertThat(DpaDiskIntegrate.firstNonBlank("System Disk", "Samsung SSD 870 QVO 1TB"))
                .isEqualTo("System Disk");
    }

    @Test
    void defaultsMissingManufacturerToUnknown() {
        assertThat(DpaDiskIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaDiskIntegrate.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaDiskIntegrate.defaultUnknown(" Samsung ")).isEqualTo("Samsung");
    }
}
