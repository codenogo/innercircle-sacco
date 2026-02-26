package com.innercircle.sacco.config.dto;

import java.util.List;

public record ConfigHealthResponse(
        boolean healthy,
        List<String> missingKeys,
        List<String> invalidKeys
) {}
