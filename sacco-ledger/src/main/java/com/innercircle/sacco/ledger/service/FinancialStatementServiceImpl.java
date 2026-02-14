package com.innercircle.sacco.ledger.service;

import com.innercircle.sacco.ledger.dto.BalanceSheetResponse;
import com.innercircle.sacco.ledger.dto.IncomeStatementResponse;
import com.innercircle.sacco.ledger.dto.TrialBalanceResponse;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.AccountType;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialStatementServiceImpl implements FinancialStatementService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceResponse generateTrialBalance(LocalDate asOfDate) {
        log.info("Generating trial balance as of {}", asOfDate);

        List<Account> accounts = accountRepository.findByActiveTrue();

        List<TrialBalanceResponse.AccountBalanceDto> accountBalances = accounts.stream()
                .map(account -> {
                    BigDecimal balance = account.getBalance();
                    boolean isDebitBalance = balance.compareTo(BigDecimal.ZERO) >= 0;

                    return TrialBalanceResponse.AccountBalanceDto.builder()
                            .accountCode(account.getAccountCode())
                            .accountName(account.getAccountName())
                            .accountType(account.getAccountType().name())
                            .debitBalance(isDebitBalance ? balance.toString() : "0.00")
                            .creditBalance(!isDebitBalance ? balance.abs().toString() : "0.00")
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDebits = accounts.stream()
                .map(Account::getBalance)
                .filter(balance -> balance.compareTo(BigDecimal.ZERO) >= 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = accounts.stream()
                .map(Account::getBalance)
                .filter(balance -> balance.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;

        return TrialBalanceResponse.builder()
                .asOfDate(asOfDate)
                .accounts(accountBalances)
                .totalDebits(totalDebits.toString())
                .totalCredits(totalCredits.toString())
                .balanced(balanced)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeStatementResponse generateIncomeStatement(LocalDate startDate, LocalDate endDate) {
        log.info("Generating income statement for period {} to {}", startDate, endDate);

        List<Account> revenueAccounts = accountRepository.findByAccountTypeAndActiveTrue(AccountType.REVENUE);
        List<Account> expenseAccounts = accountRepository.findByAccountTypeAndActiveTrue(AccountType.EXPENSE);

        List<IncomeStatementResponse.AccountLineItem> revenue = revenueAccounts.stream()
                .map(account -> IncomeStatementResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(account.getBalance().abs().toString())
                        .build())
                .collect(Collectors.toList());

        List<IncomeStatementResponse.AccountLineItem> expenses = expenseAccounts.stream()
                .map(account -> IncomeStatementResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(account.getBalance().toString())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalRevenue = revenueAccounts.stream()
                .map(Account::getBalance)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenseAccounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        return IncomeStatementResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .revenue(revenue)
                .expenses(expenses)
                .totalRevenue(totalRevenue.toString())
                .totalExpenses(totalExpenses.toString())
                .netIncome(netIncome.toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetResponse generateBalanceSheet(LocalDate asOfDate) {
        log.info("Generating balance sheet as of {}", asOfDate);

        List<Account> assetAccounts = accountRepository.findByAccountTypeAndActiveTrue(AccountType.ASSET);
        List<Account> liabilityAccounts = accountRepository.findByAccountTypeAndActiveTrue(AccountType.LIABILITY);
        List<Account> equityAccounts = accountRepository.findByAccountTypeAndActiveTrue(AccountType.EQUITY);

        List<BalanceSheetResponse.AccountLineItem> assets = assetAccounts.stream()
                .map(account -> BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(account.getBalance().toString())
                        .build())
                .collect(Collectors.toList());

        List<BalanceSheetResponse.AccountLineItem> liabilities = liabilityAccounts.stream()
                .map(account -> BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(account.getBalance().abs().toString())
                        .build())
                .collect(Collectors.toList());

        List<BalanceSheetResponse.AccountLineItem> equity = equityAccounts.stream()
                .map(account -> BalanceSheetResponse.AccountLineItem.builder()
                        .accountCode(account.getAccountCode())
                        .accountName(account.getAccountName())
                        .amount(account.getBalance().abs().toString())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalAssets = assetAccounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = liabilityAccounts.stream()
                .map(Account::getBalance)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEquity = equityAccounts.stream()
                .map(Account::getBalance)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        boolean balanced = totalAssets.compareTo(totalLiabilitiesAndEquity) == 0;

        return BalanceSheetResponse.builder()
                .asOfDate(asOfDate)
                .assets(assets)
                .liabilities(liabilities)
                .equity(equity)
                .totalAssets(totalAssets.toString())
                .totalLiabilities(totalLiabilities.toString())
                .totalEquity(totalEquity.toString())
                .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity.toString())
                .balanced(balanced)
                .build();
    }
}
