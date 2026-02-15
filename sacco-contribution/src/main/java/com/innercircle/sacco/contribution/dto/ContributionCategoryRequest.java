package com.innercircle.sacco.contribution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request to create or update a contribution category.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private boolean active = true;

    private boolean isMandatory = false;
}
