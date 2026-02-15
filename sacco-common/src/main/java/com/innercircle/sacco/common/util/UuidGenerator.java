package com.innercircle.sacco.common.util;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {}

    /**
     * Generate a UUID v7 (time-ordered) as per RFC 9562.
     */
    public static UUID generateV7() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
