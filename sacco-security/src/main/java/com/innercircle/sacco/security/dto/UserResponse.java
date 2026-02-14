package com.innercircle.sacco.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private Boolean enabled;
    private Boolean accountNonLocked;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
