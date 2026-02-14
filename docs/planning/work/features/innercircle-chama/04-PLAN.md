# Plan 04: System Configuration

## Goal
Implement database-backed configuration for loan products, contribution schedules, penalty rules, and system settings — all changeable at runtime via admin API.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Configuration Entities + Repository
**Files:** `sacco-config/src/main/java/com/innercircle/sacco/config/entity/SystemConfig.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/entity/LoanProductConfig.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/entity/ContributionScheduleConfig.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/entity/PenaltyRule.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/entity/InterestMethod.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/repository/SystemConfigRepository.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/repository/LoanProductConfigRepository.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/repository/ContributionScheduleConfigRepository.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/repository/PenaltyRuleRepository.java`
**Action:**
1. `SystemConfig`: key-value pairs (configKey UNIQUE, configValue, description, dataType)
2. `LoanProductConfig`: name, minAmount, maxAmount, interestRate (BigDecimal), interestMethod (REDUCING_BALANCE, FLAT_RATE), maxTermMonths, requiresGuarantor, maxGuarantors, active
3. `ContributionScheduleConfig`: name, frequency (WEEKLY, MONTHLY, BIWEEKLY), amount (BigDecimal), penaltyRate, gracePeriodDays, active
4. `PenaltyRule`: name, type (LATE_CONTRIBUTION, LATE_REPAYMENT, LOAN_DEFAULT), rate (BigDecimal), calculationMethod (PERCENTAGE, FIXED), gracePeriodDays, active
5. All entities extend BaseEntity

**Verify:**
```bash
mvn compile -pl sacco-config -q
```

**Done when:** All config entities and repositories compile.

### Task 2: Configuration Service + Admin API
**Files:** `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigService.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/service/ConfigServiceImpl.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/controller/ConfigController.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/dto/LoanProductRequest.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/dto/ContributionScheduleRequest.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/dto/PenaltyRuleRequest.java`, `sacco-config/src/main/java/com/innercircle/sacco/config/dto/ConfigResponse.java`
**Action:**
1. `ConfigService`: CRUD for all config types, getSystemConfig(key), updateSystemConfig(key, value)
2. `ConfigController` at `/api/v1/config` (ADMIN only):
   - GET/POST/PUT `/loan-products` — manage loan product configs
   - GET/POST/PUT `/contribution-schedules` — manage contribution schedules
   - GET/POST/PUT `/penalty-rules` — manage penalty rules
   - GET/PUT `/system` — system key-value configs
3. All mutations publish events for audit trail

**Verify:**
```bash
mvn compile -pl sacco-config -q
```

**Done when:** Config service and admin API compile.

### Task 3: Liquibase Changelogs + Seed Data
**Files:** `sacco-config/src/main/resources/db/changelog/config/001-create-config-tables.yaml`, `sacco-config/src/main/resources/db/changelog/config/002-seed-default-config.yaml`
**Action:**
1. Changelog 001: Create `system_config`, `loan_product_configs`, `contribution_schedule_configs`, `penalty_rules` tables
2. Changelog 002: Seed default config:
   - Default contribution schedule (Monthly, KES 1000)
   - Default loan product (Personal Loan, 12% reducing balance, 12 months max)
   - Default penalty rules (5% late contribution, 3% late repayment)
   - System configs (chama name, currency=KES, financial year start)
3. Update sacco-app's `db.changelog-master.yaml`

**Verify:**
```bash
mvn compile -pl sacco-config -q
```

**Done when:** Changelogs valid, module compiles.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-config -q
```

## Commit Message
```
feat(config): implement database-backed configuration module

- Loan product, contribution schedule, and penalty rule configs
- System key-value config store
- Admin-only REST API for runtime configuration changes
- Seed data with sensible defaults for Kenyan chama
```

---
*Planned: 2026-02-14*
