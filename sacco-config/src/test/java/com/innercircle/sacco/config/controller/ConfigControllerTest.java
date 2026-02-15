package com.innercircle.sacco.config.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.InterestMethod;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private ConfigController configController;

    // ===================== System Config Endpoint Tests =====================

    @Test
    void getAllSystemConfigs_shouldReturnOkWithConfigs() {
        SystemConfig config = new SystemConfig("key", "value", "desc");
        when(configService.getAllSystemConfigs()).thenReturn(List.of(config));

        ApiResponse<List<SystemConfig>> response = configController.getAllSystemConfigs();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
    }

    @Test
    void getAllSystemConfigs_withEmpty_shouldReturnEmptyList() {
        when(configService.getAllSystemConfigs()).thenReturn(Collections.emptyList());

        ApiResponse<List<SystemConfig>> response = configController.getAllSystemConfigs();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void getSystemConfig_shouldReturnConfig() {
        SystemConfig config = new SystemConfig("interest.rate", "12.5", "desc");
        when(configService.getSystemConfig("interest.rate")).thenReturn(config);

        ApiResponse<SystemConfig> response = configController.getSystemConfig("interest.rate");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getConfigKey()).isEqualTo("interest.rate");
    }

    @Test
    void createSystemConfig_shouldReturnCreatedConfig() {
        SystemConfig saved = new SystemConfig("new.key", "new.value", "description");
        when(configService.createSystemConfig("new.key", "new.value", "description")).thenReturn(saved);

        ApiResponse<SystemConfig> response = configController.createSystemConfig(
                "new.key", "new.value", "description");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getConfigKey()).isEqualTo("new.key");
        assertThat(response.getMessage()).isEqualTo("System config created successfully");
    }

    @Test
    void createSystemConfig_withNullDescription_shouldPass() {
        SystemConfig saved = new SystemConfig("key", "value", null);
        when(configService.createSystemConfig("key", "value", null)).thenReturn(saved);

        ApiResponse<SystemConfig> response = configController.createSystemConfig("key", "value", null);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void updateSystemConfig_shouldReturnUpdatedConfig() {
        SystemConfig updated = new SystemConfig("key", "updated-value", "desc");
        when(configService.updateSystemConfig("key", "updated-value")).thenReturn(updated);

        ApiResponse<SystemConfig> response = configController.updateSystemConfig(
                "key", Map.of("configValue", "updated-value"));

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getConfigValue()).isEqualTo("updated-value");
        assertThat(response.getMessage()).isEqualTo("System config updated successfully");
    }

    // ===================== Loan Product Config Endpoint Tests =====================

    @Test
    void getAllLoanProducts_withoutActiveOnly_shouldReturnAll() {
        LoanProductConfig product = new LoanProductConfig();
        product.setName("Product 1");
        when(configService.getAllLoanProducts()).thenReturn(List.of(product));

        ApiResponse<List<LoanProductConfig>> response = configController.getAllLoanProducts(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(configService).getAllLoanProducts();
    }

    @Test
    void getAllLoanProducts_withActiveOnlyTrue_shouldReturnActive() {
        LoanProductConfig product = new LoanProductConfig();
        product.setName("Active Product");
        when(configService.getActiveLoanProducts()).thenReturn(List.of(product));

        ApiResponse<List<LoanProductConfig>> response = configController.getAllLoanProducts(true);

        assertThat(response.isSuccess()).isTrue();
        verify(configService).getActiveLoanProducts();
    }

    @Test
    void getAllLoanProducts_withActiveOnlyFalse_shouldReturnAll() {
        when(configService.getAllLoanProducts()).thenReturn(Collections.emptyList());

        configController.getAllLoanProducts(false);

        verify(configService).getAllLoanProducts();
    }

    @Test
    void getLoanProduct_shouldReturnProduct() {
        UUID id = UUID.randomUUID();
        LoanProductConfig product = new LoanProductConfig();
        product.setName("Personal Loan");
        when(configService.getLoanProduct(id)).thenReturn(product);

        ApiResponse<LoanProductConfig> response = configController.getLoanProduct(id);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Personal Loan");
    }

    @Test
    void createLoanProduct_shouldReturnCreated() {
        LoanProductRequest request = LoanProductRequest.builder()
                .name("New Loan Product")
                .interestMethod(InterestMethod.REDUCING_BALANCE)
                .annualInterestRate(new BigDecimal("12.00"))
                .maxTermMonths(24)
                .maxAmount(new BigDecimal("500000"))
                .requiresGuarantor(true)
                .active(true)
                .build();

        LoanProductConfig saved = new LoanProductConfig();
        saved.setName("New Loan Product");
        when(configService.createLoanProduct(request)).thenReturn(saved);

        ApiResponse<LoanProductConfig> response = configController.createLoanProduct(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Loan product created successfully");
    }

    @Test
    void updateLoanProduct_shouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        LoanProductRequest request = LoanProductRequest.builder()
                .name("Updated Product")
                .interestMethod(InterestMethod.FLAT_RATE)
                .annualInterestRate(new BigDecimal("10.00"))
                .maxTermMonths(12)
                .maxAmount(new BigDecimal("200000"))
                .active(true)
                .build();

        LoanProductConfig updated = new LoanProductConfig();
        updated.setName("Updated Product");
        when(configService.updateLoanProduct(eq(id), eq(request))).thenReturn(updated);

        ApiResponse<LoanProductConfig> response = configController.updateLoanProduct(id, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Loan product updated successfully");
    }

    @Test
    void deleteLoanProduct_shouldCallServiceDelete() {
        UUID id = UUID.randomUUID();

        configController.deleteLoanProduct(id);

        verify(configService).deleteLoanProduct(id);
    }

    // ===================== Contribution Schedule Endpoint Tests =====================

    @Test
    void getAllContributionSchedules_withoutActiveOnly_shouldReturnAll() {
        ContributionScheduleConfig schedule = new ContributionScheduleConfig();
        schedule.setName("Monthly");
        when(configService.getAllContributionSchedules()).thenReturn(List.of(schedule));

        ApiResponse<List<ContributionScheduleConfig>> response =
                configController.getAllContributionSchedules(null);

        assertThat(response.isSuccess()).isTrue();
        verify(configService).getAllContributionSchedules();
    }

    @Test
    void getAllContributionSchedules_withActiveOnlyTrue_shouldReturnActive() {
        when(configService.getActiveContributionSchedules()).thenReturn(Collections.emptyList());

        configController.getAllContributionSchedules(true);

        verify(configService).getActiveContributionSchedules();
    }

    @Test
    void getAllContributionSchedules_withActiveOnlyFalse_shouldReturnAll() {
        when(configService.getAllContributionSchedules()).thenReturn(Collections.emptyList());

        configController.getAllContributionSchedules(false);

        verify(configService).getAllContributionSchedules();
    }

    @Test
    void getContributionSchedule_shouldReturnSchedule() {
        UUID id = UUID.randomUUID();
        ContributionScheduleConfig schedule = new ContributionScheduleConfig();
        schedule.setName("Weekly");
        when(configService.getContributionSchedule(id)).thenReturn(schedule);

        ApiResponse<ContributionScheduleConfig> response = configController.getContributionSchedule(id);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Weekly");
    }

    @Test
    void createContributionSchedule_shouldReturnCreated() {
        ContributionScheduleRequest request = ContributionScheduleRequest.builder()
                .name("Monthly Savings")
                .frequency(ContributionScheduleConfig.Frequency.MONTHLY)
                .amount(new BigDecimal("5000"))
                .penaltyEnabled(true)
                .active(true)
                .build();

        ContributionScheduleConfig saved = new ContributionScheduleConfig();
        saved.setName("Monthly Savings");
        when(configService.createContributionSchedule(request)).thenReturn(saved);

        ApiResponse<ContributionScheduleConfig> response =
                configController.createContributionSchedule(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Contribution schedule created successfully");
    }

    @Test
    void updateContributionSchedule_shouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        ContributionScheduleRequest request = ContributionScheduleRequest.builder()
                .name("Updated Schedule")
                .frequency(ContributionScheduleConfig.Frequency.WEEKLY)
                .amount(new BigDecimal("2000"))
                .active(true)
                .build();

        ContributionScheduleConfig updated = new ContributionScheduleConfig();
        updated.setName("Updated Schedule");
        when(configService.updateContributionSchedule(eq(id), eq(request))).thenReturn(updated);

        ApiResponse<ContributionScheduleConfig> response =
                configController.updateContributionSchedule(id, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Contribution schedule updated successfully");
    }

    @Test
    void deleteContributionSchedule_shouldCallServiceDelete() {
        UUID id = UUID.randomUUID();

        configController.deleteContributionSchedule(id);

        verify(configService).deleteContributionSchedule(id);
    }

    // ===================== Penalty Rule Endpoint Tests =====================

    @Test
    void getAllPenaltyRules_withoutActiveOnly_shouldReturnAll() {
        PenaltyRule rule = new PenaltyRule();
        rule.setName("Late Payment");
        when(configService.getAllPenaltyRules()).thenReturn(List.of(rule));

        ApiResponse<List<PenaltyRule>> response = configController.getAllPenaltyRules(null);

        assertThat(response.isSuccess()).isTrue();
        verify(configService).getAllPenaltyRules();
    }

    @Test
    void getAllPenaltyRules_withActiveOnlyTrue_shouldReturnActive() {
        when(configService.getActivePenaltyRules()).thenReturn(Collections.emptyList());

        configController.getAllPenaltyRules(true);

        verify(configService).getActivePenaltyRules();
    }

    @Test
    void getAllPenaltyRules_withActiveOnlyFalse_shouldReturnAll() {
        when(configService.getAllPenaltyRules()).thenReturn(Collections.emptyList());

        configController.getAllPenaltyRules(false);

        verify(configService).getAllPenaltyRules();
    }

    @Test
    void getPenaltyRule_shouldReturnRule() {
        UUID id = UUID.randomUUID();
        PenaltyRule rule = new PenaltyRule();
        rule.setName("Default Penalty");
        when(configService.getPenaltyRule(id)).thenReturn(rule);

        ApiResponse<PenaltyRule> response = configController.getPenaltyRule(id);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Default Penalty");
    }

    @Test
    void createPenaltyRule_shouldReturnCreated() {
        PenaltyRuleRequest request = PenaltyRuleRequest.builder()
                .name("Late Penalty")
                .penaltyType(PenaltyRule.PenaltyType.LATE_CONTRIBUTION)
                .rate(new BigDecimal("5.00"))
                .calculationMethod(PenaltyRule.CalculationMethod.PERCENTAGE)
                .active(true)
                .build();

        PenaltyRule saved = new PenaltyRule();
        saved.setName("Late Penalty");
        when(configService.createPenaltyRule(request)).thenReturn(saved);

        ApiResponse<PenaltyRule> response = configController.createPenaltyRule(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Penalty rule created successfully");
    }

    @Test
    void updatePenaltyRule_shouldReturnUpdated() {
        UUID id = UUID.randomUUID();
        PenaltyRuleRequest request = PenaltyRuleRequest.builder()
                .name("Updated Rule")
                .penaltyType(PenaltyRule.PenaltyType.LOAN_DEFAULT)
                .rate(new BigDecimal("10.00"))
                .calculationMethod(PenaltyRule.CalculationMethod.FLAT)
                .active(false)
                .build();

        PenaltyRule updated = new PenaltyRule();
        updated.setName("Updated Rule");
        when(configService.updatePenaltyRule(eq(id), eq(request))).thenReturn(updated);

        ApiResponse<PenaltyRule> response = configController.updatePenaltyRule(id, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Penalty rule updated successfully");
    }

    @Test
    void deletePenaltyRule_shouldCallServiceDelete() {
        UUID id = UUID.randomUUID();

        configController.deletePenaltyRule(id);

        verify(configService).deletePenaltyRule(id);
    }
}
