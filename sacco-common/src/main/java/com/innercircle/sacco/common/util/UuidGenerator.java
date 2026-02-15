package com.innercircle.sacco.common.util;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {}

    /**
     * Generate a UUID v7 (time-ordered) as per RFC 9562.
     */
    private static final com.fasterxml.uuid.NoArgGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * Generate a UUID v7 (time-ordered) as per RFC 9562.
     */
    public static UUID generateV7() {
        return GENERATOR.generate();
    }
}
