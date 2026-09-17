package com.itmsg.device42.integration.d42maximo.asset.logicaldrive;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LogicalDriveImportTest {

    @Test
    void roundsSizesToTargetScale() {
        assertThat(LogicalDriveMapper.roundSize(new BigDecimal("2048.126")))
                .isEqualByComparingTo("2048.13");
        assertThat(LogicalDriveMapper.roundSize(null)).isNull();
    }

    @Test
    void mapsAttachedNetworkNameOnlyForNfs() {
        assertThat(LogicalDriveMapper.attachedNetworkName("server:/share", "nfs"))
                .isEqualTo("server:/share");
        assertThat(LogicalDriveMapper.attachedNetworkName("server:/share", "NFS4"))
                .isEqualTo("server:/share");
        assertThat(LogicalDriveMapper.attachedNetworkName("/dev/sda1", "ext4"))
                .isNull();
        assertThat(LogicalDriveMapper.attachedNetworkName("server:/share", null))
                .isNull();
    }

    @Test
    void truncatesVolumeLabelToTargetLength() {
        assertThat(LogicalDriveMapper.truncate("12345678901234567", 16))
                .isEqualTo("1234567890123456");
        assertThat(LogicalDriveMapper.truncate(" DATA ", 16))
                .isEqualTo("DATA");
        assertThat(LogicalDriveMapper.truncate(" ", 16)).isNull();
    }
}
