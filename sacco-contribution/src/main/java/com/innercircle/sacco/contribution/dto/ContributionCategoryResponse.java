package com.innercircle.sacco.contribution.dto;

import com.innercircle.sacco.contribution.entity.ContributionCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionCategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private boolean isMandatory;

    public static ContributionCategoryResponse fromEntity(ContributionCategory category) {
        return new ContributionCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.isMandatory()
        );
    }
}
