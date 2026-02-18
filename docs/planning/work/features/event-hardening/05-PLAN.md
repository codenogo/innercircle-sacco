# Plan 05: Refactor all services to use EventOutboxWriter instead of ApplicationEventPublisher.publishEvent()

## Goal
Refactor all services to use EventOutboxWriter instead of ApplicationEventPublisher.publishEvent()

## Tasks

### Task 1: Refactor loan module services to use EventOutboxWriter
**Files:** `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBatchServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanReversalServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanBenefitServiceImpl.java`, `sacco-loan/src/main/java/com/innercircle/sacco/loan/service/LoanPenaltyServiceImpl.java`
**Action:**
Replace ApplicationEventPublisher.publishEvent(event) with EventOutboxWriter.write(event, aggregateType, aggregateId) in all loan module services. Inject EventOutboxWriter via constructor. Remove ApplicationEventPublisher injection where it is no longer used. The aggregate type should be the entity class simple name (e.g., 'LoanApplication') and aggregate ID is the entity UUID. Update corresponding test files to mock EventOutboxWriter instead of ApplicationEventPublisher.

**Verify:**
```bash
mvn -pl sacco-loan compile -q
mvn -pl sacco-loan test -q
```

**Done when:** [Observable outcome]

### Task 2: Refactor contribution, payout, and member services to use EventOutboxWriter
**Files:** `sacco-contribution/src/main/java/com/innercircle/sacco/contribution/service/ContributionServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/PayoutServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/BankWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/ShareWithdrawalServiceImpl.java`, `sacco-payout/src/main/java/com/innercircle/sacco/payout/service/CashDisbursementServiceImpl.java`, `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberServiceImpl.java`
**Action:**
Same refactoring as task 1 but for contribution, payout, and member modules. Replace eventPublisher.publishEvent() with outboxWriter.write(). Inject EventOutboxWriter. Update corresponding test files.

**Verify:**
```bash
mvn -pl sacco-contribution,sacco-payout,sacco-member compile -q
mvn -pl sacco-contribution,sacco-payout,sacco-member test -q
```

**Done when:** [Observable outcome]

### Task 3: Change event listeners from @TransactionalEventListener to @EventListener
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/FinancialEventListener.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/MemberAccountListener.java`, `sacco-audit/src/main/java/com/innercircle/sacco/audit/listener/AuditEventListener.java`
**Action:**
Change all @TransactionalEventListener annotations to @EventListener in FinancialEventListener (10 handlers), MemberAccountListener, and AuditEventListener. Remove TransactionPhase imports. Events now come from the outbox processor (not the original transaction), so transaction phase binding is no longer needed. Each handler method should remain @Transactional (handler creates its own transaction). Update corresponding test files if they reference @TransactionalEventListener.

**Verify:**
```bash
mvn -pl sacco-ledger,sacco-audit compile -q
mvn -pl sacco-ledger,sacco-audit test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout,sacco-ledger,sacco-audit compile -q
mvn -pl sacco-common,sacco-member,sacco-contribution,sacco-loan,sacco-payout,sacco-ledger,sacco-audit test -q
```

## Commit Message
```
feat(event-hardening): refactor all services to use outbox writer and change listeners to @EventListener
```
