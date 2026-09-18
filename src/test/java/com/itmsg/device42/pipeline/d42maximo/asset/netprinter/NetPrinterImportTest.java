package com.itmsg.device42.pipeline.d42maximo.asset.netprinter;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NetPrinterImportTest {

    @Test
    void roundsCurrentRamToTargetScale() {
        assertThat(NetPrinterMapper.roundCurrentRam(new BigDecimal("2.048")))
                .isEqualByComparingTo("2.05");
        assertThat(NetPrinterMapper.roundCurrentRam(null)).isNull();
    }
}
