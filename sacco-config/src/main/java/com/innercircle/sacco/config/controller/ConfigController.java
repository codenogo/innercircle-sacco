package com.innercircle.sacco.config.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.config.dto.ContributionScheduleRequest;
import com.innercircle.sacco.config.dto.LoanProductRequest;
import com.innercircle.sacco.config.dto.PenaltyRuleRequest;
import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // System Config Endpoints

    @GetMapping("/system")
    public ApiResponse<List<SystemConfig>> getAllSystemConfigs() {
        return ApiResponse.ok(configService.getAllSystemConfigs());
    }

    @GetMapping("/system/{configKey}")
    public ApiResponse<SystemConfig> getSystemConfig(@PathVariable String configKey) {
        return ApiResponse.ok(configService.getSystemConfig(configKey));
    }

    @PostMapping("/system")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SystemConfig> createSystemConfig(
            @RequestParam String configKey,
            @RequestParam String configValue,
            @RequestParam(required = false) String description) {
        return ApiResponse.ok(
                configService.createSystemConfig(configKey, configValue, description),
                "System config created successfully"
        );
    }

    @PutMapping("/system/{configKey}")
    public ApiResponse<SystemConfig> updateSystemConfig(
            @PathVariable String configKey,
            @RequestBody Map<String, String> body) {
        String configValue = body.get("configValue");
        return ApiResponse.ok(
                configService.updateSystemConfig(configKey, configValue),
                "System config updated successfully"
        );
    }

    // Loan Product Config Endpoints

    @GetMapping("/loan-products")
    public ApiResponse<List<LoanProductConfig>> getAllLoanProducts(@RequestParam(required = false) Boolean activeOnly) {
        List<LoanProductConfig> products = Boolean.TRUE.equals(activeOnly)
                ? configService.getActiveLoanProducts()
                : configService.getAllLoanProducts();
        return ApiResponse.ok(products);
    }

    @GetMapping("/loan-products/{id}")
    public ApiResponse<LoanProductConfig> getLoanProduct(@PathVariable UUID id) {
        return ApiResponse.ok(configService.getLoanProduct(id));
    }

    @PostMapping("/loan-products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoanProductConfig> createLoanProduct(@Valid @RequestBody LoanProductRequest request) {
        return ApiResponse.ok(
                configService.createLoanProduct(request),
                "Loan product created successfully"
        );
    }

    @PutMapping("/loan-products/{id}")
    public ApiResponse<LoanProductConfig> updateLoanProduct(
            @PathVariable UUID id,
            @Valid @RequestBody LoanProductRequest request) {
        return ApiResponse.ok(
                configService.updateLoanProduct(id, request),
                "Loan product updated successfully"
        );
    }

    @DeleteMapping("/loan-products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoanProduct(@PathVariable UUID id) {
        configService.deleteLoanProduct(id);
    }

    // Contribution Schedule Config Endpoints

    @GetMapping("/contribution-schedules")
    public ApiResponse<List<ContributionScheduleConfig>> getAllContributionSchedules(
            @RequestParam(required = false) Boolean activeOnly) {
        List<ContributionScheduleConfig> schedules = Boolean.TRUE.equals(activeOnly)
                ? configService.getActiveContributionSchedules()
                : configService.getAllContributionSchedules();
        return ApiResponse.ok(schedules);
    }

    @GetMapping("/contribution-schedules/{id}")
    public ApiResponse<ContributionScheduleConfig> getContributionSchedule(@PathVariable UUID id) {
        return ApiResponse.ok(configService.getContributionSchedule(id));
    }

    @PostMapping("/contribution-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContributionScheduleConfig> createContributionSchedule(
            @Valid @RequestBody ContributionScheduleRequest request) {
        return ApiResponse.ok(
                configService.createContributionSchedule(request),
                "Contribution schedule created successfully"
        );
    }

    @PutMapping("/contribution-schedules/{id}")
    public ApiResponse<ContributionScheduleConfig> updateContributionSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody ContributionScheduleRequest request) {
        return ApiResponse.ok(
                configService.updateContributionSchedule(id, request),
                "Contribution schedule updated successfully"
        );
    }

    @DeleteMapping("/contribution-schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContributionSchedule(@PathVariable UUID id) {
        configService.deleteContributionSchedule(id);
    }

    // Penalty Rule Endpoints

    @GetMapping("/penalty-rules")
    public ApiResponse<List<PenaltyRule>> getAllPenaltyRules(@RequestParam(required = false) Boolean activeOnly) {
        List<PenaltyRule> rules = Boolean.TRUE.equals(activeOnly)
                ? configService.getActivePenaltyRules()
                : configService.getAllPenaltyRules();
        return ApiResponse.ok(rules);
    }

    @GetMapping("/penalty-rules/{id}")
    public ApiResponse<PenaltyRule> getPenaltyRule(@PathVariable UUID id) {
        return ApiResponse.ok(configService.getPenaltyRule(id));
    }

    @PostMapping("/penalty-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PenaltyRule> createPenaltyRule(@Valid @RequestBody PenaltyRuleRequest request) {
        return ApiResponse.ok(
                configService.createPenaltyRule(request),
                "Penalty rule created successfully"
        );
    }

    @PutMapping("/penalty-rules/{id}")
    public ApiResponse<PenaltyRule> updatePenaltyRule(
            @PathVariable UUID id,
            @Valid @RequestBody PenaltyRuleRequest request) {
        return ApiResponse.ok(
                configService.updatePenaltyRule(id, request),
                "Penalty rule updated successfully"
        );
    }

    @DeleteMapping("/penalty-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePenaltyRule(@PathVariable UUID id) {
        configService.deletePenaltyRule(id);
    }
}
