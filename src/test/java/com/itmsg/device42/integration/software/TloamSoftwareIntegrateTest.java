package com.itmsg.device42.integration.software;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TloamSoftwareIntegrateTest {

    @Test
    void buildsUniqueIdUsingMaximoCatalogRule() {
        assertThat(TloamSoftwareIntegrate.buildUniqueId(
                " Device 42 Agent ",
                " 1. 2 ",
                " Device42 "
        )).isEqualTo("DEVICE 42 AGENT|1.2|DEVICE42");
    }

    @Test
    void defaultsMissingUniqueIdTokensToUnknown() {
        assertThat(TloamSoftwareIntegrate.buildUniqueId(null, " ", null))
                .isEqualTo("UNKNOWN|UNKNOWN|UNKNOWN");
    }
}
