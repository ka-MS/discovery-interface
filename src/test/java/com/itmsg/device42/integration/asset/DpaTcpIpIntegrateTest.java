package com.itmsg.device42.integration.asset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DpaTcpIpIntegrateTest {

    @Test
    void convertsPrefixLengthToIpv4Netmask() {
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(20)).isEqualTo("255.255.240.0");
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(22)).isEqualTo("255.255.252.0");
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(24)).isEqualTo("255.255.255.0");
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(32)).isEqualTo("255.255.255.255");
    }

    @Test
    void treatsCatchAllAndMissingPrefixAsNoNetmask() {
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(0)).isNull();
        assertThat(DpaTcpIpIntegrate.toIpv4Netmask(null)).isNull();
    }

    @Test
    void rejectsInvalidIpv4PrefixLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DpaTcpIpIntegrate.toIpv4Netmask(33));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DpaTcpIpIntegrate.toIpv4Netmask(-1));
    }
}
