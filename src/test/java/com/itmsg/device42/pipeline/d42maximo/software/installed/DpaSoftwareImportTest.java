package com.itmsg.device42.pipeline.d42maximo.software.installed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DpaSoftwareImportTest {

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(DpaSoftwareMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(DpaSoftwareMapper.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(DpaSoftwareMapper.defaultUnknown(" Device42 ")).isEqualTo("Device42");
    }
}
