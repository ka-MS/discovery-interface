package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DpaNetPrinterIntegrateTest {

    @Test
    void roundsCurrentRamToTargetScale() {
        assertThat(DpaNetPrinterIntegrate.roundCurrentRam(new BigDecimal("2.048")))
                .isEqualByComparingTo("2.05");
        assertThat(DpaNetPrinterIntegrate.roundCurrentRam(null)).isNull();
    }
}
