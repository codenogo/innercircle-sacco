package com.innercircle.sacco.config.repository;

import com.innercircle.sacco.config.entity.PenaltyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyRuleRepository extends JpaRepository<PenaltyRule, UUID> {
    List<PenaltyRule> findByActiveTrue();
    List<PenaltyRule> findByPenaltyTypeAndActiveTrue(PenaltyRule.PenaltyType penaltyType);
}
