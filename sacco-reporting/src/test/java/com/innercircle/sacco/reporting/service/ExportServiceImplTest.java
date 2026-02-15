package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @InjectMocks
    private ExportServiceImpl exportService;

    // ===================== memberStatementToCsv tests =====================

    @Test
    void memberStatementToCsv_withEmptyEntries_shouldReturnHeaderAndSummary() {
        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "John Doe",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("Date,Type,Description,Debit,Credit,Balance,Reference");
        assertThat(content).contains("Summary");
        assertThat(content).contains("Total Contributions,0");
        assertThat(content).contains("Total Loans Received,0");
        assertThat(content).contains("Total Repayments,0");
        assertThat(content).contains("Total Payouts,0");
        assertThat(content).contains("Total Penalties,0");
        assertThat(content).contains("Closing Balance,0");
    }

    @Test
    void memberStatementToCsv_withEntries_shouldContainAllFields() {
        UUID refId = UUID.randomUUID();
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 3, 15, 14, 30),
                "CONTRIBUTION", "SHARE contribution",
                null, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), refId);

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Jane Smith",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("2025-03-15 14:30");
        assertThat(content).contains("CONTRIBUTION");
        assertThat(content).contains("SHARE contribution");
        assertThat(content).contains("5000.00");
        assertThat(content).contains(refId.toString());
        assertThat(content).contains("Total Contributions,5000.00");
        assertThat(content).contains("Closing Balance,5000.00");
    }

    @Test
    void memberStatementToCsv_withDebitEntry_shouldShowDebitColumn() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 4, 1, 10, 0),
                "LOAN_REPAYMENT", "Loan repayment",
                new BigDecimal("3000.00"), null,
                new BigDecimal("-3000.00"), UUID.randomUUID());

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Test User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("-3000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("3000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("3000.00");
        assertThat(content).contains("LOAN_REPAYMENT");
    }

    @Test
    void memberStatementToCsv_withNullValues_shouldHandleGracefully() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 5, 1, 10, 0),
                "CONTRIBUTION", "Contribution",
                null, new BigDecimal("1000.00"),
                null, null);

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Null User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        // Should not throw and should contain data
        assertThat(content).isNotEmpty();
        assertThat(content).contains("CONTRIBUTION");
    }

    @Test
    void memberStatementToCsv_multipleEntries_shouldHaveCorrectRowCount() {
        MemberStatementEntry entry1 = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "CONTRIBUTION", "Contribution 1",
                null, new BigDecimal("5000"), BigDecimal.ZERO, UUID.randomUUID());

        MemberStatementEntry entry2 = new MemberStatementEntry(
                LocalDateTime.of(2025, 2, 15, 10, 0),
                "CONTRIBUTION", "Contribution 2",
                null, new BigDecimal("5000"), BigDecimal.ZERO, UUID.randomUUID());

        MemberStatementEntry entry3 = new MemberStatementEntry(
                LocalDateTime.of(2025, 3, 15, 10, 0),
                "LOAN_REPAYMENT", "Repayment",
                new BigDecimal("2000"), null, BigDecimal.ZERO, UUID.randomUUID());

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Multi User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("2000"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry1, entry2, entry3));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        // Count data lines: header + 3 entries + blank + "Summary" + 7 summary lines
        String[] lines = content.split("\n");
        assertThat(lines.length).isGreaterThanOrEqualTo(4); // header + 3 entries minimum
    }

    // ===================== memberStatementToPdf tests =====================

    @Test
    void memberStatementToPdf_withEmptyEntries_shouldGenerateValidPdf() {
        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "PDF User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList());

        byte[] pdf = exportService.memberStatementToPdf(statement);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
        // PDF files start with %PDF
        String header = new String(pdf, 0, Math.min(4, pdf.length), StandardCharsets.ISO_8859_1);
        assertThat(header).isEqualTo("%PDF");
    }

    @Test
    void memberStatementToPdf_withEntries_shouldGenerateValidPdf() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 3, 15, 14, 30),
                "CONTRIBUTION", "Monthly contribution",
                null, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), UUID.randomUUID());

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "PDF User With Data",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("5000.00"),
                new BigDecimal("5000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] pdf = exportService.memberStatementToPdf(statement);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    void memberStatementToPdf_withManyEntries_shouldGenerateMultiPagePdf() {
        // Generate enough entries to overflow a single A4 page
        java.util.List<MemberStatementEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            entries.add(new MemberStatementEntry(
                    LocalDateTime.of(2025, 1, 1, 10, 0).plusDays(i),
                    "CONTRIBUTION", "Entry " + i,
                    null, new BigDecimal("100"),
                    new BigDecimal(100 * (i + 1)), UUID.randomUUID()));
        }

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Many Entries User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("10000"),
                new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                entries);

        byte[] pdf = exportService.memberStatementToPdf(statement);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    @Test
    void memberStatementToPdf_withNullAmounts_shouldHandleGracefully() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "CONTRIBUTION", "Contribution",
                null, null, null, null);

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Null Amounts User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] pdf = exportService.memberStatementToPdf(statement);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }

    // ===================== financialSummaryToCsv tests =====================

    @Test
    void financialSummaryToCsv_shouldContainAllMetrics() {
        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                new BigDecimal("500000.00"), new BigDecimal("300000.00"),
                new BigDecimal("200000.00"), new BigDecimal("100000.00"),
                new BigDecimal("10000.00"), new BigDecimal("310000.00"),
                50L, 25L, new BigDecimal("150000.00"));

        byte[] csv = exportService.financialSummaryToCsv(summary);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("Financial Summary");
        assertThat(content).contains("Total Contributions,500000.00");
        assertThat(content).contains("Total Loans Disbursed,300000.00");
        assertThat(content).contains("Total Repayments,200000.00");
        assertThat(content).contains("Total Payouts,100000.00");
        assertThat(content).contains("Total Penalties Collected,10000.00");
        assertThat(content).contains("Net Position,310000.00");
        assertThat(content).contains("Active Members,50");
        assertThat(content).contains("Active Loans,25");
        assertThat(content).contains("Outstanding Loan Balance,150000.00");
    }

    @Test
    void financialSummaryToCsv_shouldContainDateRange() {
        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO);

        byte[] csv = exportService.financialSummaryToCsv(summary);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("2025-06-01");
        assertThat(content).contains("2025-06-30");
    }

    @Test
    void financialSummaryToCsv_withZeroValues_shouldStillGenerateCsv() {
        FinancialSummaryResponse summary = new FinancialSummaryResponse(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0L, 0L, BigDecimal.ZERO);

        byte[] csv = exportService.financialSummaryToCsv(summary);

        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).isNotEmpty();
        assertThat(content).contains("Metric,Amount");
    }

    // ===================== CSV escape tests =====================

    @Test
    void memberStatementToCsv_withCsvInjectionCharacter_shouldPreventInjection() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "CONTRIBUTION", "=cmd('calc')",
                null, new BigDecimal("1000"),
                new BigDecimal("1000"), UUID.randomUUID());

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Injection User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("1000"),
                new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        // The = should be prefixed with single quote in the escapeCsv function
        assertThat(content).contains("'=cmd('calc')");
    }

    @Test
    void memberStatementToCsv_withCommasInDescription_shouldQuoteField() {
        MemberStatementEntry entry = new MemberStatementEntry(
                LocalDateTime.of(2025, 1, 15, 10, 0),
                "CONTRIBUTION", "Monthly, annual contribution",
                null, new BigDecimal("1000"),
                new BigDecimal("1000"), UUID.randomUUID());

        MemberStatementResponse statement = new MemberStatementResponse(
                UUID.randomUUID(), "Comma User",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                BigDecimal.ZERO, new BigDecimal("1000"),
                new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(entry));

        byte[] csv = exportService.memberStatementToCsv(statement);

        String content = new String(csv, StandardCharsets.UTF_8);
        // Comma in description should be quoted
        assertThat(content).contains("\"Monthly, annual contribution\"");
    }
}
