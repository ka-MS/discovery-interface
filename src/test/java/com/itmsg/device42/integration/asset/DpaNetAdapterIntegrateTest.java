package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DpaNetAdapterIntegrateTest {

    @Test
    void splitsPortSpeedIntoValueAndUnit() {
        assertThat(DpaNetAdapterIntegrate.parseBandwidth("1000 Mbps"))
                .isEqualByComparingTo("1000.00");
        assertThat(DpaNetAdapterIntegrate.parseBandwidthUnit("1000 Mbps"))
                .isEqualTo("Mbps");
    }

    @Test
    void returnsNullWhenPortSpeedIsBlankOrNotNumeric() {
        assertThat(DpaNetAdapterIntegrate.parseBandwidth(null)).isNull();
        assertThat(DpaNetAdapterIntegrate.parseBandwidth(" ")).isNull();
        assertThat(DpaNetAdapterIntegrate.parseBandwidth("auto Mbps")).isNull();
        assertThat(DpaNetAdapterIntegrate.parseBandwidthUnit("1000")).isNull();
    }

    @Test
    void upperCasesMacAddress() {
        assertThat(DpaNetAdapterIntegrate.upperToNull(" 842519c49c88 "))
                .isEqualTo("842519C49C88");
        assertThat(DpaNetAdapterIntegrate.upperToNull(" ")).isNull();
    }

    @Test
    void truncatesPortToTargetLength() {
        assertThat(DpaNetAdapterIntegrate.truncate("0123456789abcdefgh", 16))
                .isEqualTo("0123456789abcdef");
        assertThat(DpaNetAdapterIntegrate.truncate("eth0", 16)).isEqualTo("eth0");
        assertThat(DpaNetAdapterIntegrate.truncate(null, 16)).isNull();
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaNetAdapterIntegrate.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaNetAdapterIntegrate.defaultUnknown(" Intel ")).isEqualTo("Intel");
    }
}
