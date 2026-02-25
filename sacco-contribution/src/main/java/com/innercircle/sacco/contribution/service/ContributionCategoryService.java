package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.contribution.entity.ContributionCategory;
import java.util.List;
import java.util.UUID;

public interface ContributionCategoryService {
    ContributionCategory createCategory(String name, String description, boolean mandatory, boolean welfareEligible);
    ContributionCategory updateCategory(UUID id, String name, String description, boolean active, boolean mandatory, boolean welfareEligible);
    ContributionCategory getCategory(UUID id);
    List<ContributionCategory> getAllCategories();
    List<ContributionCategory> getActiveCategories();
    void deleteCategory(UUID id);
}
