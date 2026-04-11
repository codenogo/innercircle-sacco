package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.common.event.ExitFeeAppliedEvent;
import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.member.dto.CreateMemberExitRequestRequest;
import com.innercircle.sacco.member.dto.ReviewMemberExitRequestRequest;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberExitInstallment;
import com.innercircle.sacco.member.entity.MemberExitRequest;
import com.innercircle.sacco.member.entity.MemberExitRequestStatus;
import com.innercircle.sacco.member.entity.MemberStatus;
import com.innercircle.sacco.member.repository.MemberExitInstallmentRepository;
import com.innercircle.sacco.member.repository.MemberExitRequestRepository;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutType;
import com.innercircle.sacco.payout.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberExitServiceImpl implements MemberExitService {

    private final MemberExitRequestRepository exitRequestRepository;
    private final MemberExitInstallmentRepository installmentRepository;
    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PayoutService payoutService;
    private final EventOutboxWriter outboxWriter;

    @Override
    @Transactional
    public MemberExitRequest createExitRequest(UUID memberId, CreateMemberExitRequestRequest request, String actor) {
        if (request.getEffectiveDate().isBefore(request.getNoticeDate())) {
            throw new BusinessException("Effective date cannot be before notice date");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
        BigDecimal gross = member.getShareBalance() != null ? member.getShareBalance() : BigDecimal.ZERO;
        BigDecimal liabilities = resolveMemberLiabilities(memberId);

        BigDecimal preFeeNet = gross.subtract(liabilities).max(BigDecimal.ZERO);
        BigDecimal exitFee = preFeeNet.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = preFeeNet.subtract(exitFee).max(BigDecimal.ZERO);

        MemberExitRequest exitRequest = new MemberExitRequest();
        exitRequest.setMemberId(memberId);
        exitRequest.setNoticeDate(request.getNoticeDate());
        exitRequest.setEffectiveDate(request.getEffectiveDate());
        exitRequest.setStatus(MemberExitRequestStatus.REQUESTED);
        exitRequest.setGrossSettlementAmount(gross);
        exitRequest.setLiabilityOffsetAmount(liabilities);
        exitRequest.setExitFeeAmount(exitFee);
        exitRequest.setNetSettlementAmount(net);
        exitRequest.setInstallmentCount(2);
        exitRequest.setInstallmentsProcessed(0);
        exitRequest.setNextInstallmentDueDate(request.getEffectiveDate().plusDays(90));
        exitRequest.setCreatedBy(actor);

        MemberExitRequest saved = exitRequestRepository.save(exitRequest);
        createInstallments(saved, actor);

        if (saved.getExitFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            outboxWriter.write(new ExitFeeAppliedEvent(
                    saved.getId(),
                    saved.getMemberId(),
                    saved.getExitFeeAmount(),
                    UUID.randomUUID(),
                    actor
            ), "MemberExitRequest", saved.getId());
        }
        return saved;
    }

    @Override
    @Transactional
    public MemberExitRequest reviewExitRequest(UUID memberId, UUID requestId, ReviewMemberExitRequestRequest request, String actor) {
        MemberExitRequest exitRequest = getExitRequest(memberId, requestId);
        exitRequest.setStatus(request.getStatus());
        exitRequest.setReviewNotes(request.getReviewNotes());
        exitRequest.setReviewedBy(actor);
        exitRequest.setReviewedAt(Instant.now());

        if (request.getStatus() == MemberExitRequestStatus.APPROVED) {
            exitRequest.setStatus(MemberExitRequestStatus.IN_PROGRESS);
        }
        return exitRequestRepository.save(exitRequest);
    }

    @Override
    @Transactional
    public MemberExitInstallment processInstallment(UUID memberId, UUID requestId, String actor, boolean isAdmin) {
        MemberExitRequest exitRequest = getExitRequest(memberId, requestId);
        if (exitRequest.getStatus() != MemberExitRequestStatus.IN_PROGRESS
                && exitRequest.getStatus() != MemberExitRequestStatus.APPROVED) {
            throw new BusinessException("Exit request is not in a processable state");
        }

        MemberExitInstallment installment = installmentRepository
                .findFirstByExitRequestIdAndProcessedFalseOrderByInstallmentNumberAsc(exitRequest.getId())
                .orElseThrow(() -> new BusinessException("All installments are already processed"));

        Payout payout = payoutService.createPayout(exitRequest.getMemberId(), installment.getAmount(), PayoutType.EXIT_SETTLEMENT, "EXIT_REQUEST", exitRequest.getId(), installment.getInstallmentNumber(), "SYSTEM");
        payoutService.approvePayout(payout.getId(), actor, "Member exit settlement installment", isAdmin);
        Payout processed = payoutService.processPayout(payout.getId(), actor);

        installment.setProcessed(true);
        installment.setProcessedAt(Instant.now());
        installment.setPayoutId(processed.getId());
        MemberExitInstallment savedInstallment = installmentRepository.save(installment);

        int processedCount = exitRequest.getInstallmentsProcessed() + 1;
        exitRequest.setInstallmentsProcessed(processedCount);
        if (processedCount >= exitRequest.getInstallmentCount()) {
            exitRequest.setStatus(MemberExitRequestStatus.COMPLETED);
            exitRequest.setNextInstallmentDueDate(null);
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
            member.setStatus(MemberStatus.DEACTIVATED);
            memberRepository.save(member);
        } else {
            MemberExitInstallment next = installmentRepository
                    .findFirstByExitRequestIdAndProcessedFalseOrderByInstallmentNumberAsc(exitRequest.getId())
                    .orElse(null);
            exitRequest.setNextInstallmentDueDate(next != null ? next.getDueDate() : null);
        }
        exitRequestRepository.save(exitRequest);

        return savedInstallment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberExitRequest> getExitRequests(UUID memberId) {
        return exitRequestRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberExitInstallment> getInstallments(UUID requestId) {
        return installmentRepository.findByExitRequestIdOrderByInstallmentNumberAsc(requestId);
    }

    private MemberExitRequest getExitRequest(UUID memberId, UUID requestId) {
        MemberExitRequest exitRequest = exitRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("MemberExitRequest", requestId));
        if (!exitRequest.getMemberId().equals(memberId)) {
            throw new BusinessException("Exit request does not belong to the provided member");
        }
        return exitRequest;
    }

    private void createInstallments(MemberExitRequest request, String actor) {
        BigDecimal firstInstallment = request.getNetSettlementAmount()
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal secondInstallment = request.getNetSettlementAmount().subtract(firstInstallment);

        MemberExitInstallment i1 = new MemberExitInstallment();
        i1.setExitRequestId(request.getId());
        i1.setInstallmentNumber(1);
        i1.setDueDate(request.getEffectiveDate().plusDays(90));
        i1.setAmount(firstInstallment);
        i1.setCreatedBy(actor);

        MemberExitInstallment i2 = new MemberExitInstallment();
        i2.setExitRequestId(request.getId());
        i2.setInstallmentNumber(2);
        i2.setDueDate(request.getEffectiveDate().plusDays(180));
        i2.setAmount(secondInstallment);
        i2.setCreatedBy(actor);

        installmentRepository.saveAll(List.of(i1, i2));
    }

    private BigDecimal resolveMemberLiabilities(UUID memberId) {
        BigDecimal outstandingLoans = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(outstanding_balance), 0) FROM loan_applications " +
                        "WHERE member_id = ? AND status IN ('REPAYING','DISBURSED','DEFAULTED')",
                BigDecimal.class,
                memberId
        );
        return outstandingLoans != null ? outstandingLoans : BigDecimal.ZERO;
    }
}
