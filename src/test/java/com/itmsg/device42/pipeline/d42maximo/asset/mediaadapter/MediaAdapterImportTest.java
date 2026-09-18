package com.itmsg.device42.pipeline.d42maximo.asset.mediaadapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaAdapterImportTest {

    @Test
    void fallsBackThroughDescriptionCandidates() {
        assertThat(MediaAdapterMapper.firstNonBlank(" ", null, "GeForce RTX 4090"))
                .isEqualTo("GeForce RTX 4090");
        assertThat(MediaAdapterMapper.firstNonBlank("VGA controller", null, "GeForce"))
                .isEqualTo("VGA controller");
        assertThat(MediaAdapterMapper.firstNonBlank(null, null, null)).isNull();
    }

    @Test
    void defaultsRequiredNamesToUnknown() {
        assertThat(MediaAdapterMapper.defaultUnknown(null)).isEqualTo("UNKNOWN");
        assertThat(MediaAdapterMapper.defaultUnknown(" ")).isEqualTo("UNKNOWN");
        assertThat(MediaAdapterMapper.defaultUnknown(" NVIDIA ")).isEqualTo("NVIDIA");
    }
}
