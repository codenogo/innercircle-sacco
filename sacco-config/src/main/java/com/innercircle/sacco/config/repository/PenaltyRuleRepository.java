package com.innercircle.sacco.config.repository;

import com.innercircle.sacco.config.entity.PenaltyRule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyRuleRepository extends JpaRepository<PenaltyRule, UUID> {
    @EntityGraph(attributePaths = "tiers")
    List<PenaltyRule> findByActiveTrue();

    @EntityGraph(attributePaths = "tiers")
    List<PenaltyRule> findByPenaltyTypeAndActiveTrue(PenaltyRule.PenaltyType penaltyType);

    @Override
    @EntityGraph(attributePaths = "tiers")
    List<PenaltyRule> findAll();
}
