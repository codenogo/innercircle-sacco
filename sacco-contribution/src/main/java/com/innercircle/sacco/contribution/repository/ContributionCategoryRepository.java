package com.innercircle.sacco.contribution.repository;

import com.innercircle.sacco.contribution.entity.ContributionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionCategoryRepository extends JpaRepository<ContributionCategory, UUID> {
    boolean existsByName(String name);

    List<ContributionCategory> findByActiveTrue();
}
