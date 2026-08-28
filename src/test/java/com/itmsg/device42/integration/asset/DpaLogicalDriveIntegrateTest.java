package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DpaLogicalDriveIntegrateTest {

    @Test
    void roundsSizesToTargetScale() {
        assertThat(DpaLogicalDriveIntegrate.roundSize(new BigDecimal("2048.126")))
                .isEqualByComparingTo("2048.13");
        assertThat(DpaLogicalDriveIntegrate.roundSize(null)).isNull();
    }

    @Test
    void mapsAttachedNetworkNameOnlyForNfs() {
        assertThat(DpaLogicalDriveIntegrate.attachedNetworkName("server:/share", "nfs"))
                .isEqualTo("server:/share");
        assertThat(DpaLogicalDriveIntegrate.attachedNetworkName("server:/share", "NFS4"))
                .isEqualTo("server:/share");
        assertThat(DpaLogicalDriveIntegrate.attachedNetworkName("/dev/sda1", "ext4"))
                .isNull();
        assertThat(DpaLogicalDriveIntegrate.attachedNetworkName("server:/share", null))
                .isNull();
    }

    @Test
    void truncatesVolumeLabelToTargetLength() {
        assertThat(DpaLogicalDriveIntegrate.truncate("12345678901234567", 16))
                .isEqualTo("1234567890123456");
        assertThat(DpaLogicalDriveIntegrate.truncate(" DATA ", 16))
                .isEqualTo("DATA");
        assertThat(DpaLogicalDriveIntegrate.truncate(" ", 16)).isNull();
    }
}
