package com.innercircle.sacco.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidGeneratorTest {

    @Test
    void generateV7_shouldReturnNonNullUuid() {
        UUID uuid = UuidGenerator.generateV7();

        assertThat(uuid).isNotNull();
    }

    @Test
    void generateV7_shouldReturnUniqueValues() {
        UUID uuid1 = UuidGenerator.generateV7();
        UUID uuid2 = UuidGenerator.generateV7();

        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    @Test
    void generateV7_shouldGenerateManyUniqueUuids() {
        Set<UUID> uuids = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            uuids.add(UuidGenerator.generateV7());
        }

        assertThat(uuids).hasSize(count);
    }

    @Test
    void generateV7_shouldBeTimeOrdered() {
        UUID uuid1 = UuidGenerator.generateV7();
        UUID uuid2 = UuidGenerator.generateV7();

        // UUID v7 are time-ordered, so comparing as strings should reflect ordering
        // The most significant bits encode the timestamp
        assertThat(uuid1.getMostSignificantBits())
                .isLessThanOrEqualTo(uuid2.getMostSignificantBits());
    }

    @Test
    void generateV7_shouldHaveCorrectVersion() {
        UUID uuid = UuidGenerator.generateV7();

        // UUID v7 has version 7 - bits 48-51 should be 0111 (7)
        int version = uuid.version();
        assertThat(version).isEqualTo(7);
    }

    @Test
    void generateV7_shouldHaveCorrectVariant() {
        UUID uuid = UuidGenerator.generateV7();

        // RFC 4122/9562 variant - should be 2
        int variant = uuid.variant();
        assertThat(variant).isEqualTo(2);
    }

    @Test
    void constructor_isPrivate() throws Exception {
        // Verify the utility class has a private constructor
        var constructor = UuidGenerator.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }
}
