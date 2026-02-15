package com.innercircle.sacco.security.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String username,
    String email,
    Boolean enabled,
    Set<String> roles,
    MemberSummary member,   // null if not linked
    Instant createdAt,
    Instant updatedAt
) {}
