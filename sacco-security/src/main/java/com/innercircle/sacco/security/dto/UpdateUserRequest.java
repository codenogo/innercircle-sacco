package com.innercircle.sacco.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private Boolean enabled;

    private Boolean accountNonLocked;

    private Set<String> roles;
}
