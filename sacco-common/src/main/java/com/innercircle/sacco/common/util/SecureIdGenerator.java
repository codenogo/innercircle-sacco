package com.innercircle.sacco.common.util;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class SecureIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private SecureIdGenerator() {}

    /**
     * Generate an ID in the format {@code PREFIX-XXXXXXXX} where X is uppercase hex.
     * Uses 4 random bytes (32 bits of entropy) from SecureRandom.
     */
    public static String generate(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("Prefix must not be null or empty");
        }
        byte[] bytes = new byte[4];
        SECURE_RANDOM.nextBytes(bytes);
        return prefix + "-" + HEX.formatHex(bytes);
    }

    /**
     * Check whether a value matches the expected format for the given prefix.
     */
    public static boolean matches(String prefix, String value) {
        if (prefix == null || value == null) {
            return false;
        }
        String expectedPrefix = prefix + "-";
        if (!value.startsWith(expectedPrefix)) {
            return false;
        }
        String hex = value.substring(expectedPrefix.length());
        return hex.length() == 8 && hex.chars().allMatch(c ->
                (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'));
    }
}
