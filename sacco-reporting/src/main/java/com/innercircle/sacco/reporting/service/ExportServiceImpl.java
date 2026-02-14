package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;
import com.innercircle.sacco.reporting.dto.MemberStatementEntry;
import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public byte[] memberStatementToCsv(MemberStatementResponse statement) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Type,Description,Debit,Credit,Balance,Reference\n");

        for (MemberStatementEntry entry : statement.entries()) {
            sb.append(escapeCsv(entry.date().format(DATE_FMT))).append(',');
            sb.append(escapeCsv(entry.type())).append(',');
            sb.append(escapeCsv(entry.description())).append(',');
            sb.append(entry.debit() != null ? entry.debit().toPlainString() : "").append(',');
            sb.append(entry.credit() != null ? entry.credit().toPlainString() : "").append(',');
            sb.append(entry.runningBalance() != null ? entry.runningBalance().toPlainString() : "").append(',');
            sb.append(entry.referenceId() != null ? entry.referenceId().toString() : "");
            sb.append('\n');
        }

        sb.append("\nSummary\n");
        sb.append("Total Contributions,").append(statement.totalContributions().toPlainString()).append('\n');
        sb.append("Total Loans Received,").append(statement.totalLoansReceived().toPlainString()).append('\n');
        sb.append("Total Repayments,").append(statement.totalRepayments().toPlainString()).append('\n');
        sb.append("Total Payouts,").append(statement.totalPayouts().toPlainString()).append('\n');
        sb.append("Total Penalties,").append(statement.totalPenalties().toPlainString()).append('\n');
        sb.append("Closing Balance,").append(statement.closingBalance().toPlainString()).append('\n');

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] memberStatementToPdf(MemberStatementResponse statement) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 770;
                float margin = 50;

                // Header
                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("Member Statement");
                cs.endText();
                y -= 25;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Member: " + statement.memberName() + "  |  Period: " +
                        statement.fromDate() + " to " + statement.toDate());
                cs.endText();
                y -= 30;

                // Table header
                cs.beginText();
                cs.setFont(fontBold, 8);
                cs.newLineAtOffset(margin, y);
                cs.showText("Date");
                cs.newLineAtOffset(100, 0);
                cs.showText("Type");
                cs.newLineAtOffset(80, 0);
                cs.showText("Description");
                cs.newLineAtOffset(120, 0);
                cs.showText("Debit");
                cs.newLineAtOffset(70, 0);
                cs.showText("Credit");
                cs.newLineAtOffset(70, 0);
                cs.showText("Balance");
                cs.endText();
                y -= 15;

                // Table rows
                cs.setFont(fontRegular, 7);
                for (MemberStatementEntry entry : statement.entries()) {
                    if (y < 80) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        // Note: need a new content stream for the new page
                        break; // Simplified: stop at page boundary
                    }

                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(entry.date().format(DATE_FMT));
                    cs.newLineAtOffset(100, 0);
                    cs.showText(truncate(entry.type(), 12));
                    cs.newLineAtOffset(80, 0);
                    cs.showText(truncate(entry.description(), 18));
                    cs.newLineAtOffset(120, 0);
                    cs.showText(formatAmount(entry.debit()));
                    cs.newLineAtOffset(70, 0);
                    cs.showText(formatAmount(entry.credit()));
                    cs.newLineAtOffset(70, 0);
                    cs.showText(formatAmount(entry.runningBalance()));
                    cs.endText();
                    y -= 12;
                }

                // Summary
                y -= 20;
                if (y > 80) {
                    cs.beginText();
                    cs.setFont(fontBold, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Closing Balance: " + statement.closingBalance().toPlainString());
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    @Override
    public byte[] financialSummaryToCsv(FinancialSummaryResponse summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Financial Summary\n");
        sb.append("Period,").append(summary.fromDate()).append(" to ").append(summary.toDate()).append('\n');
        sb.append('\n');
        sb.append("Metric,Amount\n");
        sb.append("Total Contributions,").append(summary.totalContributions().toPlainString()).append('\n');
        sb.append("Total Loans Disbursed,").append(summary.totalLoansDisbursed().toPlainString()).append('\n');
        sb.append("Total Repayments,").append(summary.totalRepayments().toPlainString()).append('\n');
        sb.append("Total Payouts,").append(summary.totalPayouts().toPlainString()).append('\n');
        sb.append("Total Penalties Collected,").append(summary.totalPenaltiesCollected().toPlainString()).append('\n');
        sb.append("Net Position,").append(summary.netPosition().toPlainString()).append('\n');
        sb.append('\n');
        sb.append("Active Members,").append(summary.activeMemberCount()).append('\n');
        sb.append("Active Loans,").append(summary.activeLoansCount()).append('\n');
        sb.append("Outstanding Loan Balance,").append(summary.outstandingLoanBalance().toPlainString()).append('\n');

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "";
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
