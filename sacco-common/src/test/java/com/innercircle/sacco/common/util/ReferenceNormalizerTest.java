package com.innercircle.sacco.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceNormalizerTest {

    @Test
    void normalizeOptional_shouldReturnNullForNullOrBlank() {
        assertThat(ReferenceNormalizer.normalizeOptional(null)).isNull();
        assertThat(ReferenceNormalizer.normalizeOptional("")).isNull();
        assertThat(ReferenceNormalizer.normalizeOptional("   ")).isNull();
    }

    @Test
    void normalizeOptional_shouldTrimAndUppercase() {
        assertThat(ReferenceNormalizer.normalizeOptional("  ref-001  ")).isEqualTo("REF-001");
        assertThat(ReferenceNormalizer.normalizeOptional("abc123")).isEqualTo("ABC123");
    }
}
