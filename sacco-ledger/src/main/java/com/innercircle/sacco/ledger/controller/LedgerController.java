package com.innercircle.sacco.ledger.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.ledger.dto.BalanceSheetResponse;
import com.innercircle.sacco.ledger.dto.IncomeStatementResponse;
import com.innercircle.sacco.ledger.dto.JournalEntryResponse;
import com.innercircle.sacco.ledger.dto.TrialBalanceResponse;
import com.innercircle.sacco.ledger.entity.Account;
import com.innercircle.sacco.ledger.entity.JournalEntry;
import com.innercircle.sacco.ledger.entity.JournalLine;
import com.innercircle.sacco.ledger.repository.AccountRepository;
import com.innercircle.sacco.ledger.repository.JournalEntryRepository;
import com.innercircle.sacco.ledger.service.FinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final FinancialStatementService financialStatementService;

    @GetMapping("/accounts")
    public ApiResponse<List<Account>> getChartOfAccounts() {
        List<Account> accounts = accountRepository.findByActiveTrue();
        return ApiResponse.ok(accounts);
    }

    @GetMapping("/journal-entries")
    public ApiResponse<Page<JournalEntryResponse>> getJournalEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate,desc") String sort) {

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<JournalEntry> entries = journalEntryRepository.findByPostedTrue(pageable);

        Page<JournalEntryResponse> response = entries.map(this::mapToResponse);
        return ApiResponse.ok(response);
    }

    @GetMapping("/trial-balance")
    public ApiResponse<TrialBalanceResponse> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        TrialBalanceResponse trialBalance = financialStatementService.generateTrialBalance(date);
        return ApiResponse.ok(trialBalance);
    }

    @GetMapping("/income-statement")
    public ApiResponse<IncomeStatementResponse> getIncomeStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        IncomeStatementResponse incomeStatement = financialStatementService.generateIncomeStatement(startDate, endDate);
        return ApiResponse.ok(incomeStatement);
    }

    @GetMapping("/balance-sheet")
    public ApiResponse<BalanceSheetResponse> getBalanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();
        BalanceSheetResponse balanceSheet = financialStatementService.generateBalanceSheet(date);
        return ApiResponse.ok(balanceSheet);
    }

    private JournalEntryResponse mapToResponse(JournalEntry entry) {
        List<JournalEntryResponse.JournalLineDto> lines = entry.getJournalLines().stream()
                .map(this::mapLineToDto)
                .collect(Collectors.toList());

        return JournalEntryResponse.builder()
                .id(entry.getId())
                .entryNumber(entry.getEntryNumber())
                .transactionDate(entry.getTransactionDate())
                .description(entry.getDescription())
                .transactionType(entry.getTransactionType())
                .referenceId(entry.getReferenceId())
                .posted(entry.isPosted())
                .postedAt(entry.getPostedAt())
                .journalLines(lines)
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private JournalEntryResponse.JournalLineDto mapLineToDto(JournalLine line) {
        return JournalEntryResponse.JournalLineDto.builder()
                .id(line.getId())
                .accountCode(line.getAccount().getAccountCode())
                .accountName(line.getAccount().getAccountName())
                .debitAmount(line.getDebitAmount().toString())
                .creditAmount(line.getCreditAmount().toString())
                .description(line.getDescription())
                .build();
    }
}
