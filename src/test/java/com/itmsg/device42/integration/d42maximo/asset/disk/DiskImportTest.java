package com.itmsg.device42.integration.d42maximo.asset.disk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DiskImportTest {

    @Test
    void roundsTotalSpaceToTargetScale() {
        assertThat(DiskMapper.roundTotalSpace(new BigDecimal("1024.126")))
                .isEqualByComparingTo("1024.13");
        assertThat(DiskMapper.roundTotalSpace(null)).isNull();
    }

    @Test
    void usesModelWhenDescriptionIsBlank() {
        assertThat(DiskMapper.firstNonBlank(" ", "Samsung SSD 870 QVO 1TB"))
                .isEqualTo("Samsung SSD 870 QVO 1TB");
        assertThat(DiskMapper.firstNonBlank("System Disk", "Samsung SSD 870 QVO 1TB"))
                .isEqualTo("System Disk");
    }

    @Test
    void defaultsMissingManufacturerToUnknown() {
        assertThat(DiskMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DiskMapper.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DiskMapper.defaultUnknown(" Samsung ")).isEqualTo("Samsung");
    }
}
