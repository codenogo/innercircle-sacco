package com.innercircle.sacco.security.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchRequest {

    @Size(min = 1, max = 255, message = "Query must be between 1 and 255 characters")
    private String query;

    private String cursor;

    @Min(value = 1, message = "Limit must be at least 1")
    @Builder.Default
    private int limit = 20;
}
