package com.innercircle.sacco.security.dto;

import java.util.UUID;

public record MemberSummary(
    UUID id,
    String firstName,
    String lastName,
    String memberNumber
) {}
