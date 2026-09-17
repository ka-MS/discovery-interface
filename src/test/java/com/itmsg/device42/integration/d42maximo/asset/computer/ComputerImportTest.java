package com.itmsg.device42.integration.d42maximo.asset.computer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ComputerImportTest {

    @Test
    void parsesObservedBiosDateFormats() {
        assertThat(ComputerMapper.parseBiosDate("2024-01-04"))
                .isEqualTo(LocalDateTime.of(2024, 1, 4, 0, 0));
        assertThat(ComputerMapper.parseBiosDate("11/12/2021"))
                .isEqualTo(LocalDateTime.of(2021, 11, 12, 0, 0));
        assertThat(ComputerMapper.parseBiosDate("2018/03/30 00:00"))
                .isEqualTo(LocalDateTime.of(2018, 3, 30, 0, 0));
    }

    @Test
    void returnsNullForMissingOrUnsupportedBiosDate() {
        assertThat(ComputerMapper.parseBiosDate(null)).isNull();
        assertThat(ComputerMapper.parseBiosDate(" ")).isNull();
        assertThat(ComputerMapper.parseBiosDate("not-a-date")).isNull();
    }
}
