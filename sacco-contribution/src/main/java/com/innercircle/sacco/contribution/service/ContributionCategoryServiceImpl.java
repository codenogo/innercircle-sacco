package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.contribution.entity.ContributionCategory;
import com.innercircle.sacco.contribution.repository.ContributionCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributionCategoryServiceImpl implements ContributionCategoryService {

    private final ContributionCategoryRepository categoryRepository;

    @Override
    @Transactional
    public ContributionCategory createCategory(String name, String description, boolean mandatory) {
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException("Category with name '" + name + "' already exists");
        }
        ContributionCategory category = new ContributionCategory(name, description, true, mandatory);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public ContributionCategory updateCategory(UUID id, String name, String description, boolean active, boolean mandatory) {
        ContributionCategory category = getCategory(id);
        
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new BusinessException("Category with name '" + name + "' already exists");
        }

        category.setName(name);
        category.setDescription(description);
        category.setActive(active);
        category.setMandatory(mandatory);
        
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionCategory getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContributionCategory", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionCategory> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        // Soft delete implementation: just set active = false
        ContributionCategory category = getCategory(id);
        category.setActive(false);
        categoryRepository.save(category);
    }
}
