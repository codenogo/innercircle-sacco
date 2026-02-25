package com.innercircle.sacco.config.repository;

import com.innercircle.sacco.config.entity.PenaltyRuleTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyRuleTierRepository extends JpaRepository<PenaltyRuleTier, UUID> {
    List<PenaltyRuleTier> findByRuleIdOrderBySequenceAsc(UUID ruleId);
}
