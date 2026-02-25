package com.innercircle.sacco.config.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.config.entity.SystemConfig;
import com.innercircle.sacco.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PolicyConfigResolver {

    private final SystemConfigRepository systemConfigRepository;

    public String requireString(String key) {
        String raw = resolveRequiredRawValue(key);
        if (raw.isBlank()) {
            throw new BusinessException("Policy config key is blank: " + key);
        }
        return raw;
    }

    public BigDecimal requireDecimal(String key) {
        String raw = resolveRequiredRawValue(key);
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid decimal value for policy config key: " + key);
        }
    }

    public BigDecimal requireNonNegativeDecimal(String key) {
        BigDecimal value = requireDecimal(key);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Policy config key must be non-negative: " + key);
        }
        return value;
    }

    public int requireInt(String key) {
        String raw = resolveRequiredRawValue(key);
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new BusinessException("Invalid integer value for policy config key: " + key);
        }
    }

    public int requireIntInRange(String key, int minInclusive, int maxInclusive) {
        int value = requireInt(key);
        if (value < minInclusive || value > maxInclusive) {
            throw new BusinessException("Policy config key out of range: " + key);
        }
        return value;
    }

    public int requireIntAtLeast(String key, int minInclusive) {
        int value = requireInt(key);
        if (value < minInclusive) {
            throw new BusinessException("Policy config key below minimum: " + key);
        }
        return value;
    }

    private String resolveRequiredRawValue(String key) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("Missing required policy config key: " + key));
        return config.getConfigValue() == null ? "" : config.getConfigValue().trim();
    }
}
