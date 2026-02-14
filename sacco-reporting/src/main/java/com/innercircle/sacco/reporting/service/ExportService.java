package com.innercircle.sacco.reporting.service;

import com.innercircle.sacco.reporting.dto.MemberStatementResponse;
import com.innercircle.sacco.reporting.dto.FinancialSummaryResponse;

public interface ExportService {

    byte[] memberStatementToCsv(MemberStatementResponse statement);

    byte[] memberStatementToPdf(MemberStatementResponse statement);

    byte[] financialSummaryToCsv(FinancialSummaryResponse summary);
}
