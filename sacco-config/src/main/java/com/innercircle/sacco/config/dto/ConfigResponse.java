package com.innercircle.sacco.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigResponse {

    private UUID id;
    private String name;
    private String configKey;
    private String configValue;
    private String description;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
