package com.innercircle.sacco.config.controller;

import com.innercircle.sacco.config.entity.LoanProductConfig;
import com.innercircle.sacco.config.entity.PenaltyRule;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = ConfigControllerSecurityTest.TestConfig.class)
class ConfigControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ConfigService configService() {
            return mock(ConfigService.class);
        }

        @Bean
        ConfigController configController(ConfigService configService) {
            return new ConfigController(configService);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private ConfigController configController;

    @org.springframework.beans.factory.annotation.Autowired
    private ConfigService configService;

    @BeforeEach
    void resetMocks() {
        reset(configService);
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void getAllSystemConfigs_memberAllowed() {
        when(configService.getAllSystemConfigs()).thenReturn(List.of());
        configController.getAllSystemConfigs();
        verify(configService).getAllSystemConfigs();
    }

    @Test
    @WithMockUser(roles = "CHAIRPERSON")
    void getSystemConfigHealth_chairpersonAllowed() {
        when(configService.getSystemConfigHealth()).thenReturn(
                new com.innercircle.sacco.config.dto.ConfigHealthResponse(true, List.of(), List.of())
        );
        configController.getSystemConfigHealth();
        verify(configService).getSystemConfigHealth();
    }

    @Test
    @WithMockUser(roles = "VICE_TREASURER")
    void getAllPenaltyRules_viceTreasurerAllowed() {
        when(configService.getAllPenaltyRules()).thenReturn(List.of());
        configController.getAllPenaltyRules(null);
        verify(configService).getAllPenaltyRules();
    }

    @Test
    @WithMockUser(roles = "TREASURER")
    void getAllLoanProducts_treasurerAllowed() {
        when(configService.getAllLoanProducts()).thenReturn(List.of());
        configController.getAllLoanProducts(null);
        verify(configService).getAllLoanProducts();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPenaltyRules_adminAllowed() {
        when(configService.getAllPenaltyRules()).thenReturn(List.of());
        configController.getAllPenaltyRules(null);
        verify(configService).getAllPenaltyRules();
    }

    @Test
    @WithMockUser(roles = "TREASURER")
    void createSystemConfig_treasurerDenied() {
        assertThatThrownBy(() -> configController.createSystemConfig("x.key", "10", "test"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(configService);
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void updateSystemConfig_memberDenied() {
        assertThatThrownBy(() -> configController.updateSystemConfig("x.key", Map.of("configValue", "12")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(configService);
    }

    @Test
    @WithMockUser(roles = "TREASURER")
    void deleteLoanProduct_treasurerDenied() {
        assertThatThrownBy(() -> configController.deleteLoanProduct(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(configService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSystemConfig_adminAllowed() {
        SystemConfig saved = new SystemConfig("x.key", "10", "test");
        when(configService.createSystemConfig("x.key", "10", "test")).thenReturn(saved);
        configController.createSystemConfig("x.key", "10", "test");
        verify(configService).createSystemConfig("x.key", "10", "test");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSystemConfig_adminAllowed() {
        SystemConfig saved = new SystemConfig("x.key", "20", "test");
        when(configService.updateSystemConfig("x.key", "20")).thenReturn(saved);
        configController.updateSystemConfig("x.key", Map.of("configValue", "20"));
        verify(configService).updateSystemConfig("x.key", "20");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteLoanProduct_adminAllowed() {
        UUID id = UUID.randomUUID();
        configController.deleteLoanProduct(id);
        verify(configService).deleteLoanProduct(id);
    }
}
