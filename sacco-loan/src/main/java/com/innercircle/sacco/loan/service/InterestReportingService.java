package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.dto.MemberInterestSummary;
import com.innercircle.sacco.loan.dto.MonthlyInterestSummary;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface InterestReportingService {

    MonthlyInterestSummary getMonthlyInterestSummary(YearMonth month);

    List<MemberInterestSummary> getMemberInterestSummary(UUID memberId);

    List<MemberInterestSummary> getPortfolioInterestArrears();
}
