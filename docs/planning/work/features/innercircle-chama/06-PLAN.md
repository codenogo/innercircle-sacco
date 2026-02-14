# Plan 06: Contribution Management

## Goal
Implement member contributions with recurring schedules, arrears tracking, late penalties, and domain events for ledger integration.

## Prerequisites
- [ ] Plan 01 complete

## Tasks

### Task 1: Contribution Entities + Repository
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/entity/Contribution.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/entity/ContributionType.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/entity/ContributionStatus.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/entity/ContributionPenalty.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/repository/ContributionRepository.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/repository/ContributionPenaltyRepository.java`
**Action:**
1. `Contribution` entity (extends BaseEntity): memberId (UUID), amount (BigDecimal), type (REGULAR, ONE_OFF, ARREARS), status (PENDING, CONFIRMED, REVERSED), contributionDate, referenceNumber, notes
2. `ContributionPenalty` entity: memberId, contributionId (nullable), amount, reason, status (PENDING, PAID, WAIVED), periodStart, periodEnd
3. Repositories with cursor pagination, filter by memberId, status, dateRange
4. Query: member contribution summary (total contributed, arrears amount, last contribution date)

**Verify:**
```bash
mvn compile -pl sacco-contribution -q
```

**Done when:** Contribution entities and repositories compile.

### Task 2: Contribution Service + Events
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionService.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionPenaltyService.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionPenaltyServiceImpl.java`
**Action:**
1. `ContributionService`: recordContribution, confirmContribution, reverseContribution, getMemberContributions (cursor-paginated), getMemberSummary, getArrears
2. On confirmContribution: publish `ContributionReceivedEvent` (memberId, amount, date, referenceNumber)
3. `ContributionPenaltyService`: calculatePenalties (for a period), applyPenalty, waivePenalty (requires TREASURER/ADMIN)
4. All mutations publish `AuditableEvent` subtypes

**Verify:**
```bash
mvn compile -pl sacco-contribution -q
```

**Done when:** Services compile with event publishing.

### Task 3: Contribution REST API + Liquibase
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/controller/ContributionController.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/dto/RecordContributionRequest.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/dto/ContributionResponse.java`, `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/dto/ContributionSummaryResponse.java`, `sacco-contribution/src/main/resources/db/changelog/contribution/001-create-contributions-table.yaml`
**Action:**
1. `ContributionController` at `/api/v1/contributions`:
   - POST `/` — record contribution (SECRETARY, TREASURER)
   - PATCH `/{id}/confirm` — confirm contribution (TREASURER)
   - PATCH `/{id}/reverse` — reverse contribution (ADMIN)
   - GET `/` — list contributions (cursor-paginated, filterable)
   - GET `/member/{memberId}/summary` — member contribution summary
   - GET `/arrears` — list members with arrears (TREASURER, ADMIN)
2. Liquibase: `contributions` table + `contribution_penalties` table with indexes
3. Update sacco-app changelog-master

**Verify:**
```bash
mvn compile -pl sacco-contribution -q
```

**Done when:** REST API and changelogs complete.

## Verification

After all tasks:
```bash
mvn compile -pl sacco-contribution -q
```

## Commit Message
```
feat(contribution): implement contribution management with arrears tracking

- Contribution recording, confirmation, reversal workflow
- Penalty calculation for late contributions
- Domain events for ledger integration
- Cursor-paginated REST API with member summaries
```

---
*Planned: 2026-02-14*
