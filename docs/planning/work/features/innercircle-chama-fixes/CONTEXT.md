# InnerCircle Chama Fixes - Implementation Context

## Summary

Fix all 8 critical blockers and key high-severity warnings identified in the code review of the innercircle-chama feature. These fixes address financial calculation bugs, security gaps, data integrity issues, and concurrency concerns.

## Reference

- Review report: `docs/planning/work/features/innercircle-chama/REVIEW.md`
- Original context: `docs/planning/work/features/innercircle-chama/CONTEXT.md`

## Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| B1: Ledger balance logic | Add `isNormalDebit()` method to `AccountType` enum; branch balance update logic by account type | Standard double-entry: ASSET/EXPENSE increase on debit (balance += debit - credit), LIABILITY/EQUITY/REVENUE increase on credit (balance += credit - debit). Enum method keeps logic co-located with type definition. |
| B2: Partial payment tracking | Add `amountPaid` field to `RepaymentSchedule` entity + Liquibase migration; update repayment allocation to track partial amounts | Enables accurate partial payment state. Schedule is marked paid only when amountPaid >= totalAmount. |
| B3: Final installment interest | Recalculate interest for last installment using the actual remaining principal (not stale value from prior iteration) | Interest must be computed on the true remaining balance at the final step, then totalAmount = principal + recalculated interest. |
| B4: Hardcoded secrets | Externalize client secrets to `application.yml` with `${OAUTH2_WEB_SECRET:changeme}` / `${OAUTH2_BATCH_SECRET:changeme}` env var placeholders | Standard Spring externalized config. Defaults are clearly placeholder values, not usable secrets. |
| B5: anyRequest().permitAll() | Change `.anyRequest().permitAll()` to `.anyRequest().authenticated()` | Any unmapped route should require authentication. This is the secure default. |
| B6: IDOR on reporting | Extract authenticated member ID from JWT `SecurityContextHolder`; MEMBER role can only access own data; TREASURER/ADMIN can access any member | Uses Spring Security's `@PreAuthorize` with a custom method or role check. Member dashboard auto-resolves from JWT principal. |
| B7: CSV injection | Prefix cell values starting with `=`, `+`, `-`, `@`, `\t`, `\r` with a single quote (`'`) inside the escaped CSV value | OWASP recommendation for CSV injection prevention. Single-quote prefix neutralizes formula execution in Excel/Sheets. |
| B8: Missing FK constraints | Single new Liquibase changeset `017-add-foreign-key-constraints.yaml` adding FK constraints to all 17 tables missing them | One changeset for all FKs with proper rollback. Uses `ON DELETE RESTRICT` (financial data should never cascade-delete). |
| W1: RSA key persistence | Load RSA keys from `application.yml` properties (`spring.security.oauth2.authorizationserver.jwt.private-key-location` / `public-key-location`) with fallback to generated keys for dev | Persistent keys survive restarts and support clustering. Dev profile falls back to auto-generated. |
| W5: UUID v7 correctness | Replace custom implementation with `com.fasterxml.uuid:java-uuid-generator` library's `Generators.timeBasedEpochGenerator()` | Battle-tested library eliminates bit manipulation risk. Already in the Maven ecosystem. |
| W6: Exception handler logging | Add `log.error("Unhandled exception", ex)` to the generic exception handler | Stack traces must be logged for debugging. Only the sanitized message goes to the client. |
| W7: Optimistic locking | Add `@Version private Long version` field to `BaseEntity` + Liquibase migration adding `version BIGINT DEFAULT 0` to all entity tables | Financial system needs concurrency protection. Hibernate `@Version` provides optimistic locking out of the box. |
| W8: Loan disbursement race | Remove the double-save; set status directly to `REPAYING` in a single save before publishing the event | Eliminates intermediate DISBURSED state visible to concurrent reads. Single atomic status transition. |
| W9: Journal entry number | Use `SELECT MAX(...)` within the same `@Transactional` with `PESSIMISTIC_WRITE` lock on a sequence row, or use a DB sequence directly | DB sequence is simpler and natively atomic. Add `journal_entry_number_seq` sequence via Liquibase. |
| W10: Interest rate bounds | Add `@DecimalMax("100.0")` to `LoanApplicationRequest.interestRate` | 100% annual rate is a reasonable upper bound for chama loans. Prevents data entry errors. |
| W11: Reference number | Replace `System.currentTimeMillis()` with `"PAY-" + UuidGenerator.generateV7().toString().substring(0, 8)` | UUID v7 prefix is time-ordered, unique, and non-predictable. |
| W18: Liquibase rollbacks | Add `rollback: dropTable` to all create-table changesets | Enables safe rollback of any migration. |
| W19: Unique constraints | Add UNIQUE constraints to `reference_number` columns on `loan_repayments`, `payouts`, `bank_withdrawals` | Prevents duplicate reference numbers at the DB level. |
| W2: Actuator/Swagger auth | Restrict actuator to `ADMIN` role; restrict Swagger to non-production profiles via `@Profile("!prod")` or require auth | Defense in depth. Actuator exposes internal state; Swagger is a development aid. |
| W15: Date validation | Add `@AssertTrue` or custom validator ensuring `fromDate` < `toDate` on report request parameters | Prevents nonsensical date ranges. |
| W17: PDF page break | Fix page break logic to open a new `PDPageContentStream` and continue rendering remaining entries | Current `break` truncates output silently. Must loop through all entries across pages. |

## Constraints

- All fixes must compile with existing `mvn compile -q` (no test infrastructure yet)
- Financial calculation fixes (B1, B2, B3) are highest priority — affect data correctness
- Security fixes (B4, B5, B6) are second priority — affect system safety
- Liquibase migrations must be additive (new changesets only, never modify existing)
- Existing API contracts should not break (response shapes unchanged)
- Module isolation rule: no new cross-module dependencies

## Fix Grouping (Plan Boundaries)

| Plan | Scope | Blockers/Warnings | Modules Touched |
|------|-------|-------------------|-----------------|
| 01 | Financial calculation fixes | B1, B3, W8, W9 | sacco-ledger, sacco-loan |
| 02 | Loan repayment tracking | B2 | sacco-loan, sacco-app (migration) |
| 03 | Security hardening | B4, B5, W1, W2, W10 | sacco-security, sacco-loan |
| 04 | Authorization & IDOR | B6, W15 | sacco-reporting |
| 05 | CSV injection & PDF fix | B7, W17 | sacco-reporting |
| 06 | Data integrity & common fixes | B8, W5, W6, W7, W11, W18, W19 | sacco-common, sacco-payout, sacco-app (migrations) |

## Related Code

- `sacco-ledger/src/main/java/.../LedgerServiceImpl.java` — B1, W9
- `sacco-ledger/src/main/java/.../AccountType.java` — B1
- `sacco-loan/src/main/java/.../LoanServiceImpl.java` — B2, W8
- `sacco-loan/src/main/java/.../RepaymentScheduleGenerator.java` — B3
- `sacco-loan/src/main/java/.../RepaymentSchedule.java` — B2
- `sacco-loan/src/main/java/.../LoanApplicationRequest.java` — W10
- `sacco-security/src/main/java/.../AuthorizationServerConfig.java` — B4, W1
- `sacco-security/src/main/java/.../SecurityConfig.java` — B5, W2
- `sacco-reporting/src/main/java/.../DashboardController.java` — B6
- `sacco-reporting/src/main/java/.../ReportController.java` — B6, W15
- `sacco-reporting/src/main/java/.../ExportController.java` — B6
- `sacco-reporting/src/main/java/.../ExportServiceImpl.java` — B7, W17
- `sacco-common/src/main/java/.../BaseEntity.java` — W7
- `sacco-common/src/main/java/.../UuidGenerator.java` — W5
- `sacco-common/src/main/java/.../GlobalExceptionHandler.java` — W6
- `sacco-payout/src/main/java/.../PayoutServiceImpl.java` — W11
- `sacco-app/src/main/resources/db/changelog/` — B8, W18, W19

---
*Discussed: 2026-02-15*
