package com.innercircle.sacco.config.service;

import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfigService {

    // System Config
    List<SystemConfig> getAllSystemConfigs();
    SystemConfig getSystemConfig(String configKey);
    SystemConfig updateSystemConfig(String configKey, String configValue);
    SystemConfig createSystemConfig(String configKey, String configValue, String description);

    // Loan Product Config
    List<LoanProductConfig> getAllLoanProducts();
    List<LoanProductConfig> getActiveLoanProducts();
    LoanProductConfig getLoanProduct(UUID id);
    LoanProductConfig createLoanProduct(LoanProductRequest request);
    LoanProductConfig updateLoanProduct(UUID id, LoanProductRequest request);
    void deleteLoanProduct(UUID id);

    // Contribution Schedule Config
    List<ContributionScheduleConfig> getAllContributionSchedules();
    List<ContributionScheduleConfig> getActiveContributionSchedules();
    ContributionScheduleConfig getContributionSchedule(UUID id);
    ContributionScheduleConfig createContributionSchedule(ContributionScheduleRequest request);
    ContributionScheduleConfig updateContributionSchedule(UUID id, ContributionScheduleRequest request);
    void deleteContributionSchedule(UUID id);

    // Penalty Rule
    List<PenaltyRule> getAllPenaltyRules();
    List<PenaltyRule> getActivePenaltyRules();
    Optional<PenaltyRule> getActivePenaltyRuleByType(PenaltyRule.PenaltyType penaltyType);
    PenaltyRule getPenaltyRule(UUID id);
    PenaltyRule createPenaltyRule(PenaltyRuleRequest request);
    PenaltyRule updatePenaltyRule(UUID id, PenaltyRuleRequest request);
    void deletePenaltyRule(UUID id);
}
