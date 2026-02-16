# Plan 03: Add unit tests for AccountSetupService, MemberCreatedEvent publishing, and MemberAccountListener

## Goal
Add unit tests for AccountSetupService, MemberCreatedEvent publishing, and MemberAccountListener

## Tasks

### Task 1: Unit tests for AccountSetupServiceImpl
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/test/java/com/innercircle/sacco/ledger/service/AccountSetupServiceImplTest.java`
**Action:**
Create unit tests using `@ExtendWith(MockitoExtension.class)`. Mock `AccountRepository`. Test cases: (1) `createMemberAccounts` creates two accounts with correct codes (`2001-M001`, `2002-M001`), types (LIABILITY), parentAccountCode, and memberId. (2) `createMemberAccounts` is idempotent — skips creation when accounts already exist. (3) `ensureLoanSubAccount` creates `1002-M001` account (ASSET) when member has existing accounts but no loan account. (4) `ensureLoanSubAccount` skips when loan account already exists. (5) `ensureLoanSubAccount` throws when no existing member accounts found (can't derive member number).

**Verify:**
```bash
mvn -pl sacco-ledger test -q
```

**Done when:** [Observable outcome]

### Task 2: Unit test for MemberCreatedEvent publishing
**CWD:** `sacco-member`
**Files:** `sacco-member/src/test/java/com/innercircle/sacco/member/service/MemberServiceImplTest.java`
**Action:**
Add a test method in the existing `MemberServiceImplTest` nested `Create` class to verify that `MemberCreatedEvent` is published after member creation. Use `ArgumentCaptor<MemberCreatedEvent>` to capture the event passed to `eventPublisher.publishEvent()`. Assert the event contains correct `memberId`, `memberNumber`, `firstName`, `lastName`.

**Verify:**
```bash
mvn -pl sacco-member test -q
```

**Done when:** [Observable outcome]

### Task 3: Unit tests for MemberAccountListener
**CWD:** `sacco-ledger`
**Files:** `sacco-ledger/src/test/java/com/innercircle/sacco/ledger/listener/MemberAccountListenerTest.java`
**Action:**
Create unit tests using `@ExtendWith(MockitoExtension.class)`. Mock `AccountSetupService`. Test cases: (1) `handleMemberCreated` calls `createMemberAccounts` with correct memberId and memberNumber. (2) `handleLoanApplication` calls `ensureLoanSubAccount` with correct memberId. Follow the existing test style of `FinancialEventListenerTest`.

**Verify:**
```bash
mvn -pl sacco-ledger test -q
```

**Done when:** [Observable outcome]

## Verification

After all tasks:
```bash
mvn -pl sacco-member,sacco-ledger test -q
```

## Commit Message
```
test(member-account-setup): add unit tests for account setup service, event, and listener
```
