package com.innercircle.sacco.contribution.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.contribution.dto.ContributionCategoryRequest;
import com.innercircle.sacco.contribution.dto.ContributionCategoryResponse;
import com.innercircle.sacco.contribution.entity.ContributionCategory;
import com.innercircle.sacco.contribution.service.ContributionCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contribution-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
public class ContributionCategoryController {

    private final ContributionCategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContributionCategoryResponse>> createCategory(
            @Valid @RequestBody ContributionCategoryRequest request) {
        ContributionCategory category = categoryService.createCategory(
                request.getName(),
                request.getDescription(),
                request.isMandatory()
        );
        return ResponseEntity.created(URI.create("/api/v1/contribution-categories/" + category.getId()))
                .body(ApiResponse.ok(ContributionCategoryResponse.fromEntity(category),
                        "Category created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContributionCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody ContributionCategoryRequest request) {
        ContributionCategory category = categoryService.updateCategory(
                id,
                request.getName(),
                request.getDescription(),
                request.isActive(),
                request.isMandatory()
        );
        return ResponseEntity.ok(ApiResponse.ok(ContributionCategoryResponse.fromEntity(category),
                "Category updated successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContributionCategoryResponse>> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                ContributionCategoryResponse.fromEntity(categoryService.getCategory(id))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContributionCategoryResponse>>> getAllCategories(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<ContributionCategory> categories = activeOnly
                ? categoryService.getActiveCategories()
                : categoryService.getAllCategories();

        List<ContributionCategoryResponse> response = categories.stream()
                .map(ContributionCategoryResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted successfully"));
    }
}
