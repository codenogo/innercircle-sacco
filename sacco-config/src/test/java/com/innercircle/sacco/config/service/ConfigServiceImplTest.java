package com.innercircle.sacco.config.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.dto.PenaltyTierRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.PenaltyRuleTier;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.repository.ContributionScheduleConfigRepository;
import com.innercircle.sacco.config.repository.LoanProductConfigRepository;
import com.innercircle.sacco.config.repository.PenaltyRuleRepository;
import com.innercircle.sacco.config.repository.SystemConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigServiceImplTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private LoanProductConfigRepository loanProductConfigRepository;

    @Mock
    private ContributionScheduleConfigRepository contributionScheduleConfigRepository;

    @Mock
    private PenaltyRuleRepository penaltyRuleRepository;

    @InjectMocks
    private ConfigServiceImpl configService;

    // ===================== System Config Tests =====================

    @Test
    void getAllSystemConfigs_shouldReturnAllConfigs() {
        SystemConfig config1 = new SystemConfig("key1", "value1", "desc1");
        SystemConfig config2 = new SystemConfig("key2", "value2", "desc2");
        when(systemConfigRepository.findAll()).thenReturn(List.of(config1, config2));

        List<SystemConfig> result = configService.getAllSystemConfigs();

        assertThat(result).hasSize(2);
        verify(systemConfigRepository).findAll();
    }

    @Test
    void getAllSystemConfigs_withNoConfigs_shouldReturnEmptyList() {
        when(systemConfigRepository.findAll()).thenReturn(Collections.emptyList());

        List<SystemConfig> result = configService.getAllSystemConfigs();

        assertThat(result).isEmpty();
    }

    @Test
    void getSystemConfig_withExistingKey_shouldReturnConfig() {
        SystemConfig config = new SystemConfig("interest.rate", "12.5", "Annual interest rate");
        when(systemConfigRepository.findByConfigKey("interest.rate")).thenReturn(Optional.of(config));

        SystemConfig result = configService.getSystemConfig("interest.rate");

        assertThat(result.getConfigKey()).isEqualTo("interest.rate");
        assertThat(result.getConfigValue()).isEqualTo("12.5");
        assertThat(result.getDescription()).isEqualTo("Annual interest rate");
    }

    @Test
    void getSystemConfig_withNonExistingKey_shouldThrowResourceNotFoundException() {
        when(systemConfigRepository.findByConfigKey("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getSystemConfig("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("System config")
                .hasMessageContaining("nonexistent");
    }

    @Test
    void updateSystemConfig_withExistingKey_shouldUpdateAndSave() {
        SystemConfig existing = new SystemConfig("key", "oldValue", "desc");
        when(systemConfigRepository.findByConfigKey("key")).thenReturn(Optional.of(existing));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        SystemConfig result = configService.updateSystemConfig("key", "newValue");

        assertThat(result.getConfigValue()).isEqualTo("newValue");
        verify(systemConfigRepository).save(existing);
    }

    @Test
    void updateSystemConfig_withNonExistingKey_shouldThrowResourceNotFoundException() {
        when(systemConfigRepository.findByConfigKey("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.updateSystemConfig("missing", "value"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createSystemConfig_withNewKey_shouldCreateAndSave() {
        when(systemConfigRepository.existsByConfigKey("new.key")).thenReturn(false);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        SystemConfig result = configService.createSystemConfig("new.key", "value", "description");

        assertThat(result.getConfigKey()).isEqualTo("new.key");
        assertThat(result.getConfigValue()).isEqualTo("value");
        assertThat(result.getDescription()).isEqualTo("description");

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("new.key");
    }

    @Test
    void createSystemConfig_withExistingKey_shouldThrowBusinessException() {
        when(systemConfigRepository.existsByConfigKey("existing.key")).thenReturn(true);

        assertThatThrownBy(() -> configService.createSystemConfig("existing.key", "value", "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("existing.key");

        verify(systemConfigRepository, never()).save(any());
    }

    @Test
    void createSystemConfig_withNullDescription_shouldSaveWithNull() {
        when(systemConfigRepository.existsByConfigKey("key")).thenReturn(false);
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        SystemConfig result = configService.createSystemConfig("key", "value", null);

        assertThat(result.getDescription()).isNull();
    }

    // ===================== Loan Product Config Tests =====================

    @Test
    void getAllLoanProducts_shouldReturnAll() {
        LoanProductConfig product = new LoanProductConfig();
        product.setName("Personal Loan");
        when(loanProductConfigRepository.findAll()).thenReturn(List.of(product));

        List<LoanProductConfig> result = configService.getAllLoanProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Personal Loan");
    }

    @Test
    void getAllLoanProducts_withNoProducts_shouldReturnEmpty() {
        when(loanProductConfigRepository.findAll()).thenReturn(Collections.emptyList());

        List<LoanProductConfig> result = configService.getAllLoanProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveLoanProducts_shouldReturnOnlyActive() {
        LoanProductConfig active = new LoanProductConfig();
        active.setName("Active Product");
        active.setActive(true);
        when(loanProductConfigRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));

        List<LoanProductConfig> result = configService.getActiveLoanProducts();

        assertThat(result).hasSize(1);
        verify(loanProductConfigRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void getLoanProduct_withExistingId_shouldReturn() {
        UUID id = UUID.randomUUID();
        LoanProductConfig product = new LoanProductConfig();
        product.setName("Emergency Loan");
        when(loanProductConfigRepository.findById(id)).thenReturn(Optional.of(product));

        LoanProductConfig result = configService.getLoanProduct(id);

        assertThat(result.getName()).isEqualTo("Emergency Loan");
    }

    @Test
    void getLoanProduct_withNonExistingId_shouldThrowResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(loanProductConfigRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getLoanProduct(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan product");
    }

    @Test
    void createLoanProduct_shouldMapFieldsAndSave() {
        LoanProductRequest request = LoanProductRequest.builder()
                .name("Personal Loan")
                .interestMethod(InterestMethod.REDUCING_BALANCE)
                .annualInterestRate(new BigDecimal("12.00"))
                .minTermMonths(6)
                .maxTermMonths(24)
                .minAmount(new BigDecimal("10000.00"))
                .maxAmount(new BigDecimal("500000.00"))
                .contributionCapPercent(new BigDecimal("75.00"))
                .poolCapAmount(new BigDecimal("300000.00"))
                .rolloverEnabled(true)
                .maxRolloverMonths(2)
                .rolloverSurchargeRate(new BigDecimal("10.00"))
                .interestAccrualEnabled(false)
                .requiresGuarantor(true)
                .active(true)
                .build();

        when(loanProductConfigRepository.save(any(LoanProductConfig.class))).thenAnswer(i -> i.getArgument(0));

        LoanProductConfig result = configService.createLoanProduct(request);

        assertThat(result.getName()).isEqualTo("Personal Loan");
        assertThat(result.getInterestMethod()).isEqualTo(InterestMethod.REDUCING_BALANCE);
        assertThat(result.getAnnualInterestRate()).isEqualByComparingTo("12.00");
        assertThat(result.getMinTermMonths()).isEqualTo(6);
        assertThat(result.getMaxTermMonths()).isEqualTo(24);
        assertThat(result.getMinAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getMaxAmount()).isEqualByComparingTo("500000.00");
        assertThat(result.getContributionCapPercent()).isEqualByComparingTo("75.00");
        assertThat(result.getPoolCapAmount()).isEqualByComparingTo("300000.00");
        assertThat(result.isRolloverEnabled()).isTrue();
        assertThat(result.getMaxRolloverMonths()).isEqualTo(2);
        assertThat(result.getRolloverSurchargeRate()).isEqualByComparingTo("10.00");
        assertThat(result.isInterestAccrualEnabled()).isFalse();
        assertThat(result.isRequiresGuarantor()).isTrue();
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createLoanProduct_withFlatRateInterest_shouldSave() {
        LoanProductRequest request = LoanProductRequest.builder()
                .name("Flat Rate Loan")
                .interestMethod(InterestMethod.FLAT_RATE)
                .annualInterestRate(new BigDecimal("10.00"))
                .minTermMonths(3)
                .maxTermMonths(12)
                .minAmount(new BigDecimal("5000.00"))
                .maxAmount(new BigDecimal("100000.00"))
                .rolloverEnabled(false)
                .interestAccrualEnabled(false)
                .requiresGuarantor(false)
                .active(true)
                .build();

        when(loanProductConfigRepository.save(any(LoanProductConfig.class))).thenAnswer(i -> i.getArgument(0));

        LoanProductConfig result = configService.createLoanProduct(request);

        assertThat(result.getInterestMethod()).isEqualTo(InterestMethod.FLAT_RATE);
        assertThat(result.isRequiresGuarantor()).isFalse();
    }

    @Test
    void updateLoanProduct_shouldUpdateExistingFields() {
        UUID id = UUID.randomUUID();
        LoanProductConfig existing = new LoanProductConfig();
        existing.setName("Old Name");
        existing.setInterestMethod(InterestMethod.FLAT_RATE);
        existing.setAnnualInterestRate(new BigDecimal("10.00"));
        existing.setMinTermMonths(3);
        existing.setMaxTermMonths(12);
        existing.setMinAmount(new BigDecimal("10000.00"));
        existing.setMaxAmount(new BigDecimal("100000.00"));
        existing.setContributionCapPercent(new BigDecimal("70.00"));
        existing.setPoolCapAmount(new BigDecimal("250000.00"));
        existing.setRolloverEnabled(false);
        existing.setInterestAccrualEnabled(false);
        existing.setRequiresGuarantor(false);
        existing.setActive(true);

        LoanProductRequest request = LoanProductRequest.builder()
                .name("Updated Name")
                .interestMethod(InterestMethod.REDUCING_BALANCE)
                .annualInterestRate(new BigDecimal("15.00"))
                .minTermMonths(6)
                .maxTermMonths(36)
                .minAmount(new BigDecimal("20000.00"))
                .maxAmount(new BigDecimal("1000000.00"))
                .contributionCapPercent(new BigDecimal("80.00"))
                .poolCapAmount(new BigDecimal("500000.00"))
                .rolloverEnabled(true)
                .maxRolloverMonths(3)
                .rolloverSurchargeRate(new BigDecimal("12.50"))
                .interestAccrualEnabled(true)
                .requiresGuarantor(true)
                .active(false)
                .build();

        when(loanProductConfigRepository.findById(id)).thenReturn(Optional.of(existing));
        when(loanProductConfigRepository.save(any(LoanProductConfig.class))).thenAnswer(i -> i.getArgument(0));

        LoanProductConfig result = configService.updateLoanProduct(id, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getInterestMethod()).isEqualTo(InterestMethod.REDUCING_BALANCE);
        assertThat(result.getAnnualInterestRate()).isEqualByComparingTo("15.00");
        assertThat(result.getMinTermMonths()).isEqualTo(6);
        assertThat(result.getMaxTermMonths()).isEqualTo(36);
        assertThat(result.getMinAmount()).isEqualByComparingTo("20000.00");
        assertThat(result.getMaxAmount()).isEqualByComparingTo("1000000.00");
        assertThat(result.getContributionCapPercent()).isEqualByComparingTo("80.00");
        assertThat(result.getPoolCapAmount()).isEqualByComparingTo("500000.00");
        assertThat(result.isRolloverEnabled()).isTrue();
        assertThat(result.getMaxRolloverMonths()).isEqualTo(3);
        assertThat(result.getRolloverSurchargeRate()).isEqualByComparingTo("12.50");
        assertThat(result.isInterestAccrualEnabled()).isTrue();
        assertThat(result.isRequiresGuarantor()).isTrue();
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updateLoanProduct_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(loanProductConfigRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.updateLoanProduct(id, LoanProductRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLoanProduct_withExistingId_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(loanProductConfigRepository.existsById(id)).thenReturn(true);

        configService.deleteLoanProduct(id);

        verify(loanProductConfigRepository).deleteById(id);
    }

    @Test
    void deleteLoanProduct_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(loanProductConfigRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> configService.deleteLoanProduct(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan product");

        verify(loanProductConfigRepository, never()).deleteById(any());
    }

    // ===================== Contribution Schedule Config Tests =====================

    @Test
    void getAllContributionSchedules_shouldReturnAll() {
        ContributionScheduleConfig schedule = new ContributionScheduleConfig();
        schedule.setName("Monthly Contribution");
        when(contributionScheduleConfigRepository.findAll()).thenReturn(List.of(schedule));

        List<ContributionScheduleConfig> result = configService.getAllContributionSchedules();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllContributionSchedules_withNoSchedules_shouldReturnEmpty() {
        when(contributionScheduleConfigRepository.findAll()).thenReturn(Collections.emptyList());

        List<ContributionScheduleConfig> result = configService.getAllContributionSchedules();

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveContributionSchedules_shouldReturnOnlyActive() {
        ContributionScheduleConfig active = new ContributionScheduleConfig();
        active.setActive(true);
        when(contributionScheduleConfigRepository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(active));

        List<ContributionScheduleConfig> result = configService.getActiveContributionSchedules();

        assertThat(result).hasSize(1);
        verify(contributionScheduleConfigRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void getContributionSchedule_withExistingId_shouldReturn() {
        UUID id = UUID.randomUUID();
        ContributionScheduleConfig schedule = new ContributionScheduleConfig();
        schedule.setName("Weekly Savings");
        when(contributionScheduleConfigRepository.findById(id)).thenReturn(Optional.of(schedule));

        ContributionScheduleConfig result = configService.getContributionSchedule(id);

        assertThat(result.getName()).isEqualTo("Weekly Savings");
    }

    @Test
    void getContributionSchedule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(contributionScheduleConfigRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getContributionSchedule(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contribution schedule");
    }

    @Test
    void createContributionSchedule_shouldMapFieldsAndSave() {
        ContributionScheduleRequest request = ContributionScheduleRequest.builder()
                .name("Monthly Savings")
                .frequency(ContributionScheduleConfig.Frequency.MONTHLY)
                .amount(new BigDecimal("5000.00"))
                .dueDayOfMonth(10)
                .gracePeriodDays(3)
                .mandatory(true)
                .expectedGrossAmount(new BigDecimal("11000.00"))
                .penaltyEnabled(true)
                .active(true)
                .build();

        when(contributionScheduleConfigRepository.save(any(ContributionScheduleConfig.class)))
                .thenAnswer(i -> i.getArgument(0));

        ContributionScheduleConfig result = configService.createContributionSchedule(request);

        assertThat(result.getName()).isEqualTo("Monthly Savings");
        assertThat(result.getFrequency()).isEqualTo(ContributionScheduleConfig.Frequency.MONTHLY);
        assertThat(result.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getDueDayOfMonth()).isEqualTo(10);
        assertThat(result.getGracePeriodDays()).isEqualTo(3);
        assertThat(result.isMandatory()).isTrue();
        assertThat(result.getExpectedGrossAmount()).isEqualByComparingTo("11000.00");
        assertThat(result.isPenaltyEnabled()).isTrue();
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createContributionSchedule_withWeeklyFrequency_shouldSave() {
        ContributionScheduleRequest request = ContributionScheduleRequest.builder()
                .name("Weekly Dues")
                .frequency(ContributionScheduleConfig.Frequency.WEEKLY)
                .amount(new BigDecimal("1000.00"))
                .dueDayOfMonth(5)
                .gracePeriodDays(0)
                .mandatory(false)
                .expectedGrossAmount(new BigDecimal("1000.00"))
                .penaltyEnabled(false)
                .active(true)
                .build();

        when(contributionScheduleConfigRepository.save(any(ContributionScheduleConfig.class)))
                .thenAnswer(i -> i.getArgument(0));

        ContributionScheduleConfig result = configService.createContributionSchedule(request);

        assertThat(result.getFrequency()).isEqualTo(ContributionScheduleConfig.Frequency.WEEKLY);
        assertThat(result.isPenaltyEnabled()).isFalse();
    }

    @Test
    void updateContributionSchedule_shouldUpdateExisting() {
        UUID id = UUID.randomUUID();
        ContributionScheduleConfig existing = new ContributionScheduleConfig();
        existing.setName("Old Schedule");

        ContributionScheduleRequest request = ContributionScheduleRequest.builder()
                .name("Updated Schedule")
                .frequency(ContributionScheduleConfig.Frequency.MONTHLY)
                .amount(new BigDecimal("10000.00"))
                .dueDayOfMonth(12)
                .gracePeriodDays(5)
                .mandatory(true)
                .expectedGrossAmount(new BigDecimal("12000.00"))
                .penaltyEnabled(true)
                .active(false)
                .build();

        when(contributionScheduleConfigRepository.findById(id)).thenReturn(Optional.of(existing));
        when(contributionScheduleConfigRepository.save(any(ContributionScheduleConfig.class)))
                .thenAnswer(i -> i.getArgument(0));

        ContributionScheduleConfig result = configService.updateContributionSchedule(id, request);

        assertThat(result.getName()).isEqualTo("Updated Schedule");
        assertThat(result.getAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.getDueDayOfMonth()).isEqualTo(12);
        assertThat(result.getGracePeriodDays()).isEqualTo(5);
        assertThat(result.isMandatory()).isTrue();
        assertThat(result.getExpectedGrossAmount()).isEqualByComparingTo("12000.00");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updateContributionSchedule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(contributionScheduleConfigRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.updateContributionSchedule(id,
                ContributionScheduleRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteContributionSchedule_withExistingId_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(contributionScheduleConfigRepository.existsById(id)).thenReturn(true);

        configService.deleteContributionSchedule(id);

        verify(contributionScheduleConfigRepository).deleteById(id);
    }

    @Test
    void deleteContributionSchedule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(contributionScheduleConfigRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> configService.deleteContributionSchedule(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Contribution schedule");

        verify(contributionScheduleConfigRepository, never()).deleteById(any());
    }

    // ===================== Penalty Rule Tests =====================

    @Test
    void getAllPenaltyRules_shouldReturnAll() {
        PenaltyRule rule = new PenaltyRule();
        rule.setName("Late Payment");
        when(penaltyRuleRepository.findAll()).thenReturn(List.of(rule));

        List<PenaltyRule> result = configService.getAllPenaltyRules();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllPenaltyRules_withNoRules_shouldReturnEmpty() {
        when(penaltyRuleRepository.findAll()).thenReturn(Collections.emptyList());

        List<PenaltyRule> result = configService.getAllPenaltyRules();

        assertThat(result).isEmpty();
    }

    @Test
    void getActivePenaltyRules_shouldReturnOnlyActive() {
        PenaltyRule active = new PenaltyRule();
        active.setActive(true);
        when(penaltyRuleRepository.findByActiveTrue()).thenReturn(List.of(active));

        List<PenaltyRule> result = configService.getActivePenaltyRules();

        assertThat(result).hasSize(1);
        verify(penaltyRuleRepository).findByActiveTrue();
    }

    @Test
    void getPenaltyRule_withExistingId_shouldReturn() {
        UUID id = UUID.randomUUID();
        PenaltyRule rule = new PenaltyRule();
        rule.setName("Loan Default Penalty");
        when(penaltyRuleRepository.findById(id)).thenReturn(Optional.of(rule));

        PenaltyRule result = configService.getPenaltyRule(id);

        assertThat(result.getName()).isEqualTo("Loan Default Penalty");
    }

    @Test
    void getPenaltyRule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(penaltyRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.getPenaltyRule(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Penalty rule");
    }

    @Test
    void createPenaltyRule_withLateContributionType_shouldMapAndSave() {
        PenaltyRuleRequest request = PenaltyRuleRequest.builder()
                .name("Late Contribution Penalty")
                .penaltyType(PenaltyRule.PenaltyType.LATE_CONTRIBUTION)
                .rate(new BigDecimal("5.00"))
                .calculationMethod(PenaltyRule.CalculationMethod.PERCENTAGE)
                .tiers(List.of(
                        defaultTier(1, 1, 60, PenaltyRuleTier.PenaltyFrequency.MONTHLY,
                                PenaltyRule.CalculationMethod.PERCENTAGE, "10.00", null, true),
                        defaultTier(2, 61, 120, PenaltyRuleTier.PenaltyFrequency.DAILY,
                                PenaltyRule.CalculationMethod.FLAT, "100.00", null, true)))
                .active(true)
                .build();

        when(penaltyRuleRepository.save(any(PenaltyRule.class))).thenAnswer(i -> i.getArgument(0));

        PenaltyRule result = configService.createPenaltyRule(request);

        assertThat(result.getName()).isEqualTo("Late Contribution Penalty");
        assertThat(result.getPenaltyType()).isEqualTo(PenaltyRule.PenaltyType.LATE_CONTRIBUTION);
        assertThat(result.getRate()).isEqualByComparingTo("10.00");
        assertThat(result.getCalculationMethod()).isEqualTo(PenaltyRule.CalculationMethod.PERCENTAGE);
        assertThat(result.getTiers()).hasSize(2);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createPenaltyRule_withLoanDefaultType_shouldMapAndSave() {
        PenaltyRuleRequest request = PenaltyRuleRequest.builder()
                .name("Loan Default Flat Penalty")
                .penaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT)
                .rate(new BigDecimal("500.00"))
                .calculationMethod(PenaltyRule.CalculationMethod.FLAT)
                .tiers(List.of(defaultTier(1, 1, 90, PenaltyRuleTier.PenaltyFrequency.MONTHLY,
                        PenaltyRule.CalculationMethod.FLAT, "500.00", 1, true)))
                .active(true)
                .build();

        when(penaltyRuleRepository.save(any(PenaltyRule.class))).thenAnswer(i -> i.getArgument(0));

        PenaltyRule result = configService.createPenaltyRule(request);

        assertThat(result.getPenaltyType()).isEqualTo(PenaltyRule.PenaltyType.LOAN_DEFAULT);
        assertThat(result.getCalculationMethod()).isEqualTo(PenaltyRule.CalculationMethod.FLAT);
        assertThat(result.getRate()).isEqualByComparingTo("500.00");
    }

    @Test
    void updatePenaltyRule_shouldUpdateExisting() {
        UUID id = UUID.randomUUID();
        PenaltyRule existing = new PenaltyRule();
        existing.setName("Old Rule");

        PenaltyRuleRequest request = PenaltyRuleRequest.builder()
                .name("Updated Rule")
                .penaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT)
                .rate(new BigDecimal("10.00"))
                .calculationMethod(PenaltyRule.CalculationMethod.PERCENTAGE)
                .tiers(List.of(defaultTier(1, 1, null, PenaltyRuleTier.PenaltyFrequency.ONCE,
                        PenaltyRule.CalculationMethod.PERCENTAGE, "10.00", 1, true)))
                .active(false)
                .build();

        when(penaltyRuleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(penaltyRuleRepository.save(any(PenaltyRule.class))).thenAnswer(i -> i.getArgument(0));

        PenaltyRule result = configService.updatePenaltyRule(id, request);

        assertThat(result.getName()).isEqualTo("Updated Rule");
        assertThat(result.getPenaltyType()).isEqualTo(PenaltyRule.PenaltyType.LOAN_DEFAULT);
        assertThat(result.getRate()).isEqualByComparingTo("10.00");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updatePenaltyRule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(penaltyRuleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configService.updatePenaltyRule(id, PenaltyRuleRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePenaltyRule_withExistingId_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(penaltyRuleRepository.existsById(id)).thenReturn(true);

        configService.deletePenaltyRule(id);

        verify(penaltyRuleRepository).deleteById(id);
    }

    @Test
    void deletePenaltyRule_withNonExistingId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(penaltyRuleRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> configService.deletePenaltyRule(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Penalty rule");

        verify(penaltyRuleRepository, never()).deleteById(any());
    }

    private PenaltyTierRequest defaultTier(
            int sequence,
            int startOverdueDay,
            Integer endOverdueDay,
            PenaltyRuleTier.PenaltyFrequency frequency,
            PenaltyRule.CalculationMethod calculationMethod,
            String rate,
            Integer maxApplications,
            boolean active
    ) {
        return PenaltyTierRequest.builder()
                .sequence(sequence)
                .startOverdueDay(startOverdueDay)
                .endOverdueDay(endOverdueDay)
                .frequency(frequency)
                .calculationMethod(calculationMethod)
                .rate(new BigDecimal(rate))
                .maxApplications(maxApplications)
                .active(active)
                .build();
    }
}
