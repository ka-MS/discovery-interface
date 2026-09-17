package com.itmsg.device42.integration.d42maximo.software.catalog;

import com.itmsg.device42.integration.d42maximo.software.SoftwareIdentity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TloamSoftwareImportTest {

    @Test
    void buildsUniqueIdUsingMaximoCatalogRule() {
        assertThat(SoftwareIdentity.buildUniqueId(
                " Device 42 Agent ",
                " 1. 2 ",
                " Device42 "
        )).isEqualTo("DEVICE 42 AGENT|1.2|DEVICE42");
    }

    @Test
    void defaultsMissingUniqueIdTokensToUnknown() {
        assertThat(SoftwareIdentity.buildUniqueId(null, " ", null))
                .isEqualTo("UNKNOWN|UNKNOWN|UNKNOWN");
    }
}
