package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountSetupServiceImpl implements AccountSetupService {

    private static final String PARENT_MEMBER_SHARES = "2001";
    private static final String PARENT_MEMBER_SAVINGS = "2002";
    private static final String PARENT_LOAN_RECEIVABLE = "1002";

    private final AccountRepository accountRepository;

    @Override
    public void createMemberAccounts(UUID memberId, String memberNumber) {
        createSubAccountIfAbsent(memberId, PARENT_MEMBER_SHARES,
                PARENT_MEMBER_SHARES + "-" + memberNumber,
                "Member Shares - " + memberNumber,
                AccountType.LIABILITY);

        createSubAccountIfAbsent(memberId, PARENT_MEMBER_SAVINGS,
                PARENT_MEMBER_SAVINGS + "-" + memberNumber,
                "Member Savings - " + memberNumber,
                AccountType.LIABILITY);

        log.info("Member accounts ensured for member {} ({})", memberId, memberNumber);
    }

    @Override
    public void ensureLoanSubAccount(UUID memberId) {
        List<Account> memberAccounts = accountRepository.findByMemberId(memberId);
        if (memberAccounts.isEmpty()) {
            throw new IllegalStateException("No existing accounts found for member " + memberId);
        }

        String memberNumber = extractMemberNumber(memberAccounts.get(0).getAccountCode());

        createSubAccountIfAbsent(memberId, PARENT_LOAN_RECEIVABLE,
                PARENT_LOAN_RECEIVABLE + "-" + memberNumber,
                "Loan Receivable - " + memberNumber,
                AccountType.ASSET);

        log.info("Loan sub-account ensured for member {} ({})", memberId, memberNumber);
    }

    private void createSubAccountIfAbsent(UUID memberId, String parentAccountCode,
                                           String accountCode, String accountName,
                                           AccountType accountType) {
        if (accountRepository.existsByMemberIdAndParentAccountCode(memberId, parentAccountCode)) {
            log.debug("Account already exists: {} for member {}", accountCode, memberId);
            return;
        }

        Account account = new Account();
        account.setAccountCode(accountCode);
        account.setAccountName(accountName);
        account.setAccountType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        account.setParentAccountCode(parentAccountCode);
        account.setMemberId(memberId);

        accountRepository.save(account);
        log.info("Created sub-account: {} ({})", accountCode, accountType);
    }

    private String extractMemberNumber(String accountCode) {
        int dashIndex = accountCode.indexOf('-');
        if (dashIndex < 0) {
            throw new IllegalStateException("Cannot derive member number from account code: " + accountCode);
        }
        return accountCode.substring(dashIndex + 1);
    }
}
