package com.itmsg.device42.integration.d42maximo.asset.os;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OsImportTest {

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(OsMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(OsMapper.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(OsMapper.defaultUnknown(" Canonical ")).isEqualTo("Canonical");
    }
}
