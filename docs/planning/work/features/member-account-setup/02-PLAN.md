# Plan 02: Create AccountSetupService, wire MemberCreatedEvent publishing, and add event listeners for account creation

## Goal
Create AccountSetupService, wire MemberCreatedEvent publishing, and add event listeners for account creation

## Tasks

### Task 1: Create AccountSetupService
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/AccountSetupService.java`, `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/service/AccountSetupServiceImpl.java`
**Action:**
Create `AccountSetupService` interface and `AccountSetupServiceImpl`. Methods: (1) `createMemberAccounts(UUID memberId, String memberNumber)` — creates two sub-accounts: Member Shares `2001-{memberNumber}` (LIABILITY) and Member Savings `2002-{memberNumber}` (LIABILITY). Skips if accounts already exist (idempotent via `existsByMemberIdAndParentAccountCode`). (2) `ensureLoanSubAccount(UUID memberId)` — looks up existing member accounts by `memberId` to derive the member number suffix from account code, then creates Loan Receivable `1002-{memberNumber}` (ASSET) if it doesn't exist. Use `AccountRepository` for persistence. Set `parentAccountCode` and `memberId` on created accounts. Use constructor injection.

**Verify:**
```bash
mvn -pl sacco-ledger compile -q
```

**Done when:** [Observable outcome]

### Task 2: Publish MemberCreatedEvent from MemberServiceImpl
**CWD:** `sacco-member`
**Files:** `sacco-member/src/main/java/com/innercircle/sacco/member/service/MemberServiceImpl.java`
**Action:**
Replace the TODO comment at lines 41-42 in `MemberServiceImpl.create()` with actual event publishing: `eventPublisher.publishEvent(new MemberCreatedEvent(savedMember.getId(), savedMember.getMemberNumber(), savedMember.getFirstName(), savedMember.getLastName(), "system"))`. Add the import for MemberCreatedEvent.

**Verify:**
```bash
mvn -pl sacco-member compile -q
```

**Done when:** [Observable outcome]

### Task 3: Add MemberAccountListener for event handling
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/main/java/com/innercircle/sacco/ledger/listener/MemberAccountListener.java`
**Action:**
Create `MemberAccountListener` in sacco-ledger listener package. (1) `@TransactionalEventListener(phase = BEFORE_COMMIT) handleMemberCreated(MemberCreatedEvent)` — calls `accountSetupService.createMemberAccounts(event.memberId(), event.memberNumber())`. (2) `@TransactionalEventListener(phase = BEFORE_COMMIT) handleLoanApplication(LoanApplicationEvent)` — calls `accountSetupService.ensureLoanSubAccount(event.memberId())`. Use `@Component`, `@RequiredArgsConstructor`, `@Slf4j`. Inject `AccountSetupService`. Follow the same pattern as `FinancialEventListener`.

**Verify:**
```bash
mvn -pl sacco-ledger compile -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-common,sacco-member,sacco-ledger compile -q
```

## Commit Message
```
feat(member-account-setup): wire AccountSetupService, MemberCreatedEvent, and listeners
```
