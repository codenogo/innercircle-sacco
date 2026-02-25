package com.innercircle.sacco.config.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.config.dto.ConfigHealthResponse;
import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.dto.PenaltyTierRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.PenaltyRuleTier;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.repository.ContributionScheduleConfigRepository;
import com.innercircle.sacco.config.repository.LoanProductConfigRepository;
import com.innercircle.sacco.config.repository.PenaltyRuleRepository;
import com.innercircle.sacco.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConfigServiceImpl implements ConfigService {

    private static final List<String> REQUIRED_POLICY_KEYS = List.of(
            "chama.name",
            "chama.currency",
            "chama.financial_year_start_month",
            "contribution.monthly.gross_amount",
            "contribution.welfare.fixed_amount",
            "contribution.due_day_of_month",
            "contribution.late_penalty.daily_flat",
            "member.arrears.cessation.consecutive_months",
            "loan.short.pool_cap",
            "loan.short.min_amount",
            "loan.short.max_amount",
            "loan.short.term_months",
            "loan.short.interest_rate",
            "loan.short.rollover.max_months",
            "loan.short.rollover.surcharge_rate",
            "loan.medium.min_amount",
            "loan.medium.max_amount",
            "loan.medium.min_term_months",
            "loan.medium.max_term_months",
            "loan.medium.interest_rate",
            "loan.medium.contribution_cap_percent",
            "loan.penalty.grace_period_days",
            "loan.penalty.default_threshold_days",
            "meeting.fines.absence_amount",
            "meeting.fines.late_threshold_minutes",
            "meeting.fines.late_amount",
            "meeting.fines.unpaid_daily_penalty",
            "welfare.cooling_off_months"
    );

    private final SystemConfigRepository systemConfigRepository;
    private final LoanProductConfigRepository loanProductConfigRepository;
    private final ContributionScheduleConfigRepository contributionScheduleConfigRepository;
    private final PenaltyRuleRepository penaltyRuleRepository;

    // System Config

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfig> getAllSystemConfigs() {
        return systemConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemConfig getSystemConfig(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("System config", configKey));
    }

    @Override
    public SystemConfig updateSystemConfig(String configKey, String configValue) {
        SystemConfig config = getSystemConfig(configKey);
        config.setConfigValue(configValue);
        SystemConfig saved = systemConfigRepository.save(config);
        log.info("Updated system config: {} = {}", configKey, configValue);
        return saved;
    }

    @Override
    public SystemConfig createSystemConfig(String configKey, String configValue, String description) {
        if (systemConfigRepository.existsByConfigKey(configKey)) {
            throw new BusinessException("System config already exists: " + configKey);
        }
        SystemConfig config = new SystemConfig();
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setDescription(description);
        SystemConfig saved = systemConfigRepository.save(config);
        log.info("Created system config: {} = {}", configKey, configValue);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfigHealthResponse getSystemConfigHealth() {
        Map<String, String> configByKey = new HashMap<>();
        for (SystemConfig cfg : systemConfigRepository.findAll()) {
            configByKey.put(cfg.getConfigKey(), cfg.getConfigValue());
        }

        List<String> missing = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        for (String key : REQUIRED_POLICY_KEYS) {
            String rawValue = configByKey.get(key);
            if (rawValue == null || rawValue.isBlank()) {
                missing.add(key);
                continue;
            }
            if (!isValidPolicyValue(key, rawValue)) {
                invalid.add(key);
            }
        }

        return new ConfigHealthResponse(missing.isEmpty() && invalid.isEmpty(), missing, invalid);
    }

    // Loan Product Config

    @Override
    @Transactional(readOnly = true)
    public List<LoanProductConfig> getAllLoanProducts() {
        return loanProductConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanProductConfig> getActiveLoanProducts() {
        return loanProductConfigRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanProductConfig getLoanProduct(UUID id) {
        return loanProductConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product", id));
    }

    @Override
    public LoanProductConfig createLoanProduct(LoanProductRequest request) {
        LoanProductConfig config = new LoanProductConfig();
        mapLoanProductRequest(request, config);
        LoanProductConfig saved = loanProductConfigRepository.save(config);
        log.info("Created loan product config: {}", saved.getName());
        return saved;
    }

    @Override
    public LoanProductConfig updateLoanProduct(UUID id, LoanProductRequest request) {
        LoanProductConfig config = getLoanProduct(id);
        mapLoanProductRequest(request, config);
        LoanProductConfig saved = loanProductConfigRepository.save(config);
        log.info("Updated loan product config: {}", saved.getName());
        return saved;
    }

    @Override
    public void deleteLoanProduct(UUID id) {
        if (!loanProductConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("Loan product", id);
        }
        loanProductConfigRepository.deleteById(id);
        log.info("Deleted loan product config: {}", id);
    }

    private void mapLoanProductRequest(LoanProductRequest request, LoanProductConfig config) {
        if (request.getMinAmount() == null || request.getMaxAmount() == null) {
            throw new BusinessException("Loan product min and max amount are required");
        }
        if (request.getMinTermMonths() == null || request.getMaxTermMonths() == null) {
            throw new BusinessException("Loan product min and max term are required");
        }
        if (request.getMinAmount().compareTo(request.getMaxAmount()) > 0) {
            throw new BusinessException("Loan product min amount cannot be greater than max amount");
        }
        if (request.getMinTermMonths() > request.getMaxTermMonths()) {
            throw new BusinessException("Loan product min term cannot be greater than max term");
        }
        if (request.getContributionCapPercent() != null
                && request.getContributionCapPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Contribution cap percent cannot exceed 100");
        }
        if (request.isRolloverEnabled() && request.getMaxRolloverMonths() == null) {
            throw new BusinessException("Max rollover months is required when rollover is enabled");
        }
        if (request.isRolloverEnabled() && request.getRolloverSurchargeRate() == null) {
            throw new BusinessException("Rollover surcharge rate is required when rollover is enabled");
        }

        config.setName(request.getName());
        config.setInterestMethod(request.getInterestMethod());
        config.setAnnualInterestRate(request.getAnnualInterestRate());
        config.setMinTermMonths(request.getMinTermMonths());
        config.setMaxTermMonths(request.getMaxTermMonths());
        config.setMinAmount(request.getMinAmount());
        config.setMaxAmount(request.getMaxAmount());
        config.setContributionCapPercent(request.getContributionCapPercent());
        config.setPoolCapAmount(request.getPoolCapAmount());
        config.setRolloverEnabled(request.isRolloverEnabled());
        config.setMaxRolloverMonths(request.isRolloverEnabled() ? request.getMaxRolloverMonths() : null);
        config.setRolloverSurchargeRate(request.isRolloverEnabled() ? request.getRolloverSurchargeRate() : null);
        config.setInterestAccrualEnabled(request.isInterestAccrualEnabled());
        config.setRequiresGuarantor(request.isRequiresGuarantor());
        config.setActive(request.isActive());
    }

    // Contribution Schedule Config

    @Override
    @Transactional(readOnly = true)
    public List<ContributionScheduleConfig> getAllContributionSchedules() {
        return contributionScheduleConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContributionScheduleConfig> getActiveContributionSchedules() {
        return contributionScheduleConfigRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionScheduleConfig getContributionSchedule(UUID id) {
        return contributionScheduleConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution schedule", id));
    }

    @Override
    public ContributionScheduleConfig createContributionSchedule(ContributionScheduleRequest request) {
        ContributionScheduleConfig config = new ContributionScheduleConfig();
        mapContributionScheduleRequest(request, config);
        ContributionScheduleConfig saved = contributionScheduleConfigRepository.save(config);
        log.info("Created contribution schedule config: {}", saved.getName());
        return saved;
    }

    @Override
    public ContributionScheduleConfig updateContributionSchedule(UUID id, ContributionScheduleRequest request) {
        ContributionScheduleConfig config = getContributionSchedule(id);
        mapContributionScheduleRequest(request, config);
        ContributionScheduleConfig saved = contributionScheduleConfigRepository.save(config);
        log.info("Updated contribution schedule config: {}", saved.getName());
        return saved;
    }

    @Override
    public void deleteContributionSchedule(UUID id) {
        if (!contributionScheduleConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contribution schedule", id);
        }
        contributionScheduleConfigRepository.deleteById(id);
        log.info("Deleted contribution schedule config: {}", id);
    }

    private void mapContributionScheduleRequest(ContributionScheduleRequest request, ContributionScheduleConfig config) {
        if (request.getExpectedGrossAmount() == null) {
            throw new BusinessException("Expected gross amount is required");
        }
        if (request.getExpectedGrossAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Expected gross amount must be non-negative");
        }

        config.setName(request.getName());
        config.setFrequency(request.getFrequency());
        config.setAmount(request.getAmount());
        config.setDueDayOfMonth(request.getDueDayOfMonth());
        config.setGracePeriodDays(request.getGracePeriodDays());
        config.setMandatory(request.isMandatory());
        config.setExpectedGrossAmount(request.getExpectedGrossAmount());
        config.setPenaltyEnabled(request.isPenaltyEnabled());
        config.setActive(request.isActive());
    }

    // Penalty Rule

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyRule> getAllPenaltyRules() {
        return penaltyRuleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyRule> getActivePenaltyRules() {
        return penaltyRuleRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PenaltyRule> getActivePenaltyRuleByType(PenaltyRule.PenaltyType penaltyType) {
        List<PenaltyRule> rules = penaltyRuleRepository.findByPenaltyTypeAndActiveTrue(penaltyType);
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.getFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public PenaltyRule getPenaltyRule(UUID id) {
        return penaltyRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty rule", id));
    }

    @Override
    public PenaltyRule createPenaltyRule(PenaltyRuleRequest request) {
        PenaltyRule rule = new PenaltyRule();
        mapPenaltyRuleRequest(request, rule);
        PenaltyRule saved = penaltyRuleRepository.save(rule);
        log.info("Created penalty rule: {}", saved.getName());
        return saved;
    }

    @Override
    public PenaltyRule updatePenaltyRule(UUID id, PenaltyRuleRequest request) {
        PenaltyRule rule = getPenaltyRule(id);
        mapPenaltyRuleRequest(request, rule);
        PenaltyRule saved = penaltyRuleRepository.save(rule);
        log.info("Updated penalty rule: {}", saved.getName());
        return saved;
    }

    @Override
    public void deletePenaltyRule(UUID id) {
        if (!penaltyRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Penalty rule", id);
        }
        penaltyRuleRepository.deleteById(id);
        log.info("Deleted penalty rule: {}", id);
    }

    private void mapPenaltyRuleRequest(PenaltyRuleRequest request, PenaltyRule rule) {
        List<PenaltyTierRequest> normalizedTiers = normalizeAndValidateTiers(request.getTiers());

        rule.setName(request.getName());
        rule.setPenaltyType(request.getPenaltyType());
        rule.setActive(request.isActive());

        rule.getTiers().clear();
        for (PenaltyTierRequest tierRequest : normalizedTiers) {
            PenaltyRuleTier tier = new PenaltyRuleTier();
            tier.setRule(rule);
            tier.setSequence(tierRequest.getSequence());
            tier.setStartOverdueDay(tierRequest.getStartOverdueDay());
            tier.setEndOverdueDay(tierRequest.getEndOverdueDay());
            tier.setFrequency(tierRequest.getFrequency());
            tier.setCalculationMethod(tierRequest.getCalculationMethod());
            tier.setRate(tierRequest.getRate());
            tier.setMaxApplications(tierRequest.getMaxApplications());
            tier.setActive(tierRequest.isActive());
            rule.getTiers().add(tier);
        }

        PenaltyTierRequest canonicalTier = normalizedTiers.stream()
                .filter(PenaltyTierRequest::isActive)
                .findFirst()
                .orElse(normalizedTiers.getFirst());
        rule.setRate(canonicalTier.getRate());
        rule.setCalculationMethod(canonicalTier.getCalculationMethod());
    }

    private List<PenaltyTierRequest> normalizeAndValidateTiers(List<PenaltyTierRequest> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            throw new BusinessException("At least one penalty tier is required");
        }

        List<PenaltyTierRequest> normalized = new ArrayList<>(tiers);
        normalized.sort(Comparator.comparing(PenaltyTierRequest::getSequence));

        Set<Integer> seenSequences = new HashSet<>();
        int previousSequence = 0;
        for (PenaltyTierRequest tier : normalized) {
            if (tier.getSequence() == null
                    || tier.getStartOverdueDay() == null
                    || tier.getFrequency() == null
                    || tier.getCalculationMethod() == null
                    || tier.getRate() == null) {
                throw new BusinessException(
                        "Penalty tier sequence, start overdue day, frequency, calculation method, and rate are required"
                );
            }
            if (!seenSequences.add(tier.getSequence())) {
                throw new BusinessException("Penalty tier sequence values must be unique");
            }
            if (tier.getSequence() <= previousSequence) {
                throw new BusinessException("Penalty tier sequence must be strictly increasing");
            }
            if (tier.getEndOverdueDay() != null && tier.getEndOverdueDay() < tier.getStartOverdueDay()) {
                throw new BusinessException("Penalty tier end overdue day cannot be less than start overdue day");
            }
            previousSequence = tier.getSequence();
        }

        if (normalized.getFirst().getStartOverdueDay() != 1) {
            throw new BusinessException("First penalty tier must start at overdue day 1");
        }

        return normalized;
    }

    private boolean isValidPolicyValue(String key, String rawValue) {
        String value = rawValue.trim();

        return switch (key) {
            case "chama.name" -> !value.isBlank();
            case "chama.currency" -> value.matches("[A-Za-z]{3}");
            case "chama.financial_year_start_month" -> isIntInRange(value, 1, 12);

            case "contribution.monthly.gross_amount",
                    "contribution.welfare.fixed_amount",
                    "contribution.late_penalty.daily_flat",
                    "loan.short.pool_cap",
                    "loan.short.min_amount",
                    "loan.short.max_amount",
                    "loan.short.interest_rate",
                    "loan.short.rollover.surcharge_rate",
                    "loan.medium.min_amount",
                    "loan.medium.max_amount",
                    "loan.medium.interest_rate",
                    "loan.medium.contribution_cap_percent",
                    "meeting.fines.absence_amount",
                    "meeting.fines.late_amount",
                    "meeting.fines.unpaid_daily_penalty" -> isDecimalAtLeastZero(value);

            case "contribution.due_day_of_month",
                    "loan.short.term_months",
                    "loan.short.rollover.max_months",
                    "loan.medium.min_term_months",
                    "loan.medium.max_term_months",
                    "loan.penalty.grace_period_days",
                    "loan.penalty.default_threshold_days",
                    "meeting.fines.late_threshold_minutes",
                    "member.arrears.cessation.consecutive_months",
                    "welfare.cooling_off_months" -> isIntAtLeastZero(value);

            default -> true;
        };
    }

    private boolean isIntInRange(String raw, int min, int max) {
        try {
            int parsed = Integer.parseInt(raw);
            return parsed >= min && parsed <= max;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isIntAtLeastZero(String raw) {
        try {
            return Integer.parseInt(raw) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isDecimalAtLeastZero(String raw) {
        try {
            return new BigDecimal(raw).compareTo(BigDecimal.ZERO) >= 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
