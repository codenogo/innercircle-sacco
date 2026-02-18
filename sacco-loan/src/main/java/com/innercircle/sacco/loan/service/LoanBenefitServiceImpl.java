package com.innercircle.sacco.loan.service;

import com.innercircle.sacco.common.event.BenefitsDistributedEvent;
import com.innercircle.sacco.common.event.LoanRepaymentEvent;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.loan.dto.LoanBenefitResponse;
import com.innercircle.sacco.loan.dto.MemberEarningsResponse;
import com.innercircle.sacco.loan.entity.LoanBenefit;
import com.innercircle.sacco.loan.repository.LoanBenefitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanBenefitServiceImpl implements LoanBenefitService {

    private final LoanBenefitRepository benefitRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EventOutboxWriter outboxWriter;

    @EventListener
    @Transactional
    public void handleLoanRepayment(LoanRepaymentEvent event) {
        if (event.interestPortion().compareTo(BigDecimal.ZERO) > 0) {
            log.info("Distributing interest earnings for loan {} - Interest: {}",
                    event.loanId(), event.interestPortion());
            distributeInterestEarnings(event.loanId(), event.interestPortion(), event.actor());
        }
    }

    @Override
    @Transactional
    public List<LoanBenefit> distributeInterestEarnings(UUID loanId, BigDecimal interestAmount, String actor) {
        if (interestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest amount must be greater than zero");
        }

        // Query active members with their share balances
        String sql = "SELECT id, share_balance FROM members WHERE status = 'ACTIVE' AND share_balance > 0 ORDER BY id";
        List<Map<String, Object>> memberShares = jdbcTemplate.queryForList(sql);

        if (memberShares.isEmpty()) {
            log.warn("No active members with share balances found for distribution");
            return List.of();
        }

        // Calculate total shares
        BigDecimal totalShares = memberShares.stream()
                .map(row -> (BigDecimal) row.get("share_balance"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Total shares is zero or negative, cannot distribute");
            return List.of();
        }

        List<LoanBenefit> benefits = new ArrayList<>();

        // Distribute proportionally
        for (Map<String, Object> row : memberShares) {
            UUID memberId = (UUID) row.get("id");
            BigDecimal shareBalance = (BigDecimal) row.get("share_balance");

            // Calculate member's proportion
            BigDecimal benefitsRate = shareBalance
                    .divide(totalShares, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Calculate member's earnings
            BigDecimal earnedAmount = interestAmount
                    .multiply(shareBalance)
                    .divide(totalShares, 2, RoundingMode.HALF_UP);

            LoanBenefit benefit = new LoanBenefit();
            benefit.setMemberId(memberId);
            benefit.setLoanId(loanId);
            benefit.setContributionSnapshot(shareBalance);
            benefit.setBenefitsRate(benefitsRate);
            benefit.setEarnedAmount(earnedAmount);
            benefit.setExpectedEarnings(earnedAmount);
            benefit.setDistributed(true);
            benefit.setDistributedAt(Instant.now());

            benefits.add(benefit);
        }

        List<LoanBenefit> savedBenefits = benefitRepository.saveAll(benefits);

        // Publish event
        outboxWriter.write(new BenefitsDistributedEvent(
                loanId,
                interestAmount,
                savedBenefits.size(),
                UUID.randomUUID(),
                actor
        ), "LoanApplication", loanId);

        log.info("Distributed {} to {} beneficiaries from loan {}",
                interestAmount, savedBenefits.size(), loanId);

        return savedBenefits;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberEarningsResponse getMemberEarnings(UUID memberId) {
        List<LoanBenefit> benefits = benefitRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        BigDecimal totalEarnings = benefits.stream()
                .map(LoanBenefit::getEarnedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal distributedEarnings = benefits.stream()
                .filter(LoanBenefit::isDistributed)
                .map(LoanBenefit::getEarnedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingEarnings = totalEarnings.subtract(distributedEarnings);

        long distributedCount = benefits.stream()
                .filter(LoanBenefit::isDistributed)
                .count();

        long pendingCount = benefits.stream()
                .filter(b -> !b.isDistributed())
                .count();

        List<LoanBenefitResponse> benefitResponses = benefits.stream()
                .map(LoanBenefitResponse::from)
                .toList();

        return MemberEarningsResponse.builder()
                .memberId(memberId)
                .totalEarnings(totalEarnings)
                .distributedEarnings(distributedEarnings)
                .pendingEarnings(pendingEarnings)
                .totalBenefits(benefits.size())
                .distributedCount((int) distributedCount)
                .pendingCount((int) pendingCount)
                .benefits(benefitResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanBenefitResponse> getLoanBenefits(UUID loanId) {
        List<LoanBenefit> benefits = benefitRepository.findByLoanIdOrderByEarnedAmountDesc(loanId);
        return benefits.stream()
                .map(LoanBenefitResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanBenefitResponse> getAllBenefits(UUID cursor, int limit) {
        UUID actualCursor = cursor != null ? cursor : new UUID(0L, 0L);
        Pageable pageable = PageRequest.of(0, limit);

        List<LoanBenefit> benefits = benefitRepository.findByIdGreaterThanOrderById(actualCursor, pageable);
        return benefits.stream()
                .map(LoanBenefitResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<LoanBenefit> refreshBeneficiaries(UUID loanId, String actor) {
        // Get existing benefits for this loan
        List<LoanBenefit> existingBenefits = benefitRepository.findByLoanId(loanId);

        if (existingBenefits.isEmpty()) {
            log.warn("No existing benefits found for loan {}", loanId);
            return List.of();
        }

        // Calculate total expected earnings from existing benefits
        BigDecimal totalExpectedEarnings = existingBenefits.stream()
                .map(LoanBenefit::getExpectedEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mark all existing benefits as distributed if not already
        for (LoanBenefit benefit : existingBenefits) {
            if (!benefit.isDistributed()) {
                benefit.setDistributed(true);
                benefit.setDistributedAt(Instant.now());
            }
        }
        benefitRepository.saveAll(existingBenefits);

        // Redistribute the total expected earnings based on current share balances
        return distributeInterestEarnings(loanId, totalExpectedEarnings, actor);
    }
}
