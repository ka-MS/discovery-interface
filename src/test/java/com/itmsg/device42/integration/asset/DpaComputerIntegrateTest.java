package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DpaComputerIntegrateTest {

    @Test
    void parsesObservedBiosDateFormats() {
        assertThat(DpaComputerIntegrate.parseBiosDate("2024-01-04"))
                .isEqualTo(LocalDateTime.of(2024, 1, 4, 0, 0));
        assertThat(DpaComputerIntegrate.parseBiosDate("11/12/2021"))
                .isEqualTo(LocalDateTime.of(2021, 11, 12, 0, 0));
        assertThat(DpaComputerIntegrate.parseBiosDate("2018/03/30 00:00"))
                .isEqualTo(LocalDateTime.of(2018, 3, 30, 0, 0));
    }

    @Test
    void returnsNullForMissingOrUnsupportedBiosDate() {
        assertThat(DpaComputerIntegrate.parseBiosDate(null)).isNull();
        assertThat(DpaComputerIntegrate.parseBiosDate(" ")).isNull();
        assertThat(DpaComputerIntegrate.parseBiosDate("not-a-date")).isNull();
    }
}
