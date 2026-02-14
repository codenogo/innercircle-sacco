package com.innercircle.sacco.common.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidGenerator() {}

    /**
     * Generate a UUID v7 (time-ordered) as per RFC 9562.
     * Layout: 48-bit unix_ts_ms | 4-bit version(7) | 12-bit rand_a | 2-bit variant(10) | 62-bit rand_b
     */
    public static UUID generateV7() {
        long timestamp = System.currentTimeMillis();
        byte[] randomBytes = new byte[10];
        RANDOM.nextBytes(randomBytes);

        long msb = (timestamp << 16) & 0xFFFF_FFFF_FFFF_0000L;
        msb |= 0x7000L; // version 7
        msb |= ((long) randomBytes[0] & 0xFF) << 4;
        msb |= ((long) randomBytes[1] & 0x0F);

        long lsb = 0x8000_0000_0000_0000L; // variant 10
        for (int i = 2; i < 10; i++) {
            lsb |= ((long) randomBytes[i] & 0xFF) << ((9 - i) * 8);
        }
        lsb &= 0xBFFF_FFFF_FFFF_FFFFL; // ensure variant bits

        return new UUID(msb, lsb);
    }
}
