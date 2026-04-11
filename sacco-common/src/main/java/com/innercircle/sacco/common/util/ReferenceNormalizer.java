package com.innercircle.sacco.common.util;

import java.util.Locale;

public final class ReferenceNormalizer {

    private ReferenceNormalizer() {}

    public static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
