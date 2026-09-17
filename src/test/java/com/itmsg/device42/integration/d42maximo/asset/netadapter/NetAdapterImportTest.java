package com.itmsg.device42.integration.d42maximo.asset.netadapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetAdapterImportTest {

    @Test
    void splitsPortSpeedIntoValueAndUnit() {
        assertThat(NetAdapterMapper.parseBandwidth("1000 Mbps"))
                .isEqualByComparingTo("1000.00");
        assertThat(NetAdapterMapper.parseBandwidthUnit("1000 Mbps"))
                .isEqualTo("Mbps");
    }

    @Test
    void returnsNullWhenPortSpeedIsBlankOrNotNumeric() {
        assertThat(NetAdapterMapper.parseBandwidth(null)).isNull();
        assertThat(NetAdapterMapper.parseBandwidth(" ")).isNull();
        assertThat(NetAdapterMapper.parseBandwidth("auto Mbps")).isNull();
        assertThat(NetAdapterMapper.parseBandwidthUnit("1000")).isNull();
    }

    @Test
    void upperCasesMacAddress() {
        assertThat(NetAdapterMapper.upperToNull(" 842519c49c88 "))
                .isEqualTo("842519C49C88");
        assertThat(NetAdapterMapper.upperToNull(" ")).isNull();
    }

    @Test
    void truncatesPortToTargetLength() {
        assertThat(NetAdapterMapper.truncate("0123456789abcdefgh", 16))
                .isEqualTo("0123456789abcdef");
        assertThat(NetAdapterMapper.truncate("eth0", 16)).isEqualTo("eth0");
        assertThat(NetAdapterMapper.truncate(null, 16)).isNull();
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(NetAdapterMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(NetAdapterMapper.defaultUnknown(" Intel ")).isEqualTo("Intel");
    }
}
