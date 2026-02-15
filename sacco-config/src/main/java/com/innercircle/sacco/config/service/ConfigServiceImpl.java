package com.innercircle.sacco.config.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.repository.ContributionScheduleConfigRepository;
import com.innercircle.sacco.config.repository.LoanProductConfigRepository;
import com.innercircle.sacco.config.repository.PenaltyRuleRepository;
import com.innercircle.sacco.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConfigServiceImpl implements ConfigService {

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
        config.setName(request.getName());
        config.setInterestMethod(request.getInterestMethod());
        config.setAnnualInterestRate(request.getAnnualInterestRate());
        config.setMaxTermMonths(request.getMaxTermMonths());
        config.setMaxAmount(request.getMaxAmount());
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
        config.setName(request.getName());
        config.setFrequency(request.getFrequency());
        config.setAmount(request.getAmount());
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
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
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
        rule.setName(request.getName());
        rule.setPenaltyType(request.getPenaltyType());
        rule.setRate(request.getRate());
        rule.setCalculationMethod(request.getCalculationMethod());
        rule.setActive(request.isActive());
    }
}
