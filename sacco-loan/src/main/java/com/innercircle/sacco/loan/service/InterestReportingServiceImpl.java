package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.loan.dto.MemberInterestSummary;
import com.innercircle.sacco.loan.dto.MonthlyInterestSummary;
import com.innercircle.sacco.loan.entity.InterestEventType;
import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanInterestHistory;
import com.innercircle.sacco.loan.entity.LoanStatus;
import com.innercircle.sacco.loan.repository.LoanApplicationRepository;
import com.innercircle.sacco.loan.repository.LoanInterestHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterestReportingServiceImpl implements InterestReportingService {

    private final LoanInterestHistoryRepository interestHistoryRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Override
    @Transactional(readOnly = true)
    public MonthlyInterestSummary getMonthlyInterestSummary(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<LoanInterestHistory> monthlyHistory = interestHistoryRepository
                .findByAccrualDateBetween(startDate, endDate);

        BigDecimal totalAccrued = monthlyHistory.stream()
                .filter(h -> h.getEventType() == InterestEventType.DAILY_ACCRUAL)
                .map(LoanInterestHistory::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReceived = monthlyHistory.stream()
                .filter(h -> h.getEventType() == InterestEventType.REPAYMENT_APPLIED)
                .map(LoanInterestHistory::getInterestAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LoanApplication> activeLoans = loanApplicationRepository.findByStatus(LoanStatus.REPAYING);

        int loansWithArrears = 0;
        for (LoanApplication loan : activeLoans) {
            BigDecimal arrears = loan.getTotalInterestAccrued()
                    .subtract(loan.getTotalInterestPaid());
            if (arrears.compareTo(BigDecimal.ZERO) > 0) {
                loansWithArrears++;
            }
        }

        BigDecimal portfolioArrears = activeLoans.stream()
                .map(loan -> loan.getTotalInterestAccrued().subtract(loan.getTotalInterestPaid()))
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonthlyInterestSummary.builder()
                .month(month)
                .totalInterestAccrued(totalAccrued)
                .totalInterestReceived(totalReceived)
                .totalInterestArrears(portfolioArrears)
                .activeLoansCount(activeLoans.size())
                .loansWithArrearsCount(loansWithArrears)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberInterestSummary> getMemberInterestSummary(UUID memberId) {
        List<LoanApplication> memberLoans = loanApplicationRepository.findByMemberId(memberId);

        return memberLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.REPAYING
                        || loan.getStatus() == LoanStatus.CLOSED)
                .map(loan -> {
                    List<LoanInterestHistory> history = interestHistoryRepository
                            .findByLoanIdOrderByAccrualDateDesc(loan.getId());
                    LocalDate lastAccrualDate = history.stream()
                            .filter(h -> h.getEventType() == InterestEventType.DAILY_ACCRUAL)
                            .map(LoanInterestHistory::getAccrualDate)
                            .findFirst()
                            .orElse(null);

                    BigDecimal arrears = loan.getTotalInterestAccrued()
                            .subtract(loan.getTotalInterestPaid())
                            .max(BigDecimal.ZERO);

                    return MemberInterestSummary.builder()
                            .memberId(memberId)
                            .loanId(loan.getId())
                            .totalInterestAccrued(loan.getTotalInterestAccrued())
                            .totalInterestPaid(loan.getTotalInterestPaid())
                            .interestArrears(arrears)
                            .lastAccrualDate(lastAccrualDate)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberInterestSummary> getPortfolioInterestArrears() {
        List<LoanApplication> repayingLoans = loanApplicationRepository.findByStatus(LoanStatus.REPAYING);

        return repayingLoans.stream()
                .filter(loan -> loan.getTotalInterestAccrued()
                        .subtract(loan.getTotalInterestPaid())
                        .compareTo(BigDecimal.ZERO) > 0)
                .map(loan -> {
                    List<LoanInterestHistory> history = interestHistoryRepository
                            .findByLoanIdOrderByAccrualDateDesc(loan.getId());
                    LocalDate lastAccrualDate = history.stream()
                            .filter(h -> h.getEventType() == InterestEventType.DAILY_ACCRUAL)
                            .map(LoanInterestHistory::getAccrualDate)
                            .findFirst()
                            .orElse(null);

                    BigDecimal arrears = loan.getTotalInterestAccrued()
                            .subtract(loan.getTotalInterestPaid());

                    return MemberInterestSummary.builder()
                            .memberId(loan.getMemberId())
                            .loanId(loan.getId())
                            .totalInterestAccrued(loan.getTotalInterestAccrued())
                            .totalInterestPaid(loan.getTotalInterestPaid())
                            .interestArrears(arrears)
                            .lastAccrualDate(lastAccrualDate)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
