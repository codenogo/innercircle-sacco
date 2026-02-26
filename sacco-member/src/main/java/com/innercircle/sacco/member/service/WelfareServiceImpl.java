package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.config.service.PolicyConfigResolver;
import com.innercircle.sacco.member.dto.CreateWelfareBeneficiaryRequest;
import com.innercircle.sacco.member.dto.CreateWelfareClaimRequest;
import com.innercircle.sacco.member.dto.ReviewWelfareClaimRequest;
import com.innercircle.sacco.member.dto.WelfareFundSummaryResponse;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.WelfareBeneficiary;
import com.innercircle.sacco.member.entity.WelfareBenefitCatalog;
import com.innercircle.sacco.member.entity.WelfareClaim;
import com.innercircle.sacco.member.entity.WelfareClaimStatus;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.member.repository.WelfareBeneficiaryRepository;
import com.innercircle.sacco.member.repository.WelfareBenefitCatalogRepository;
import com.innercircle.sacco.member.repository.WelfareClaimRepository;
import com.innercircle.sacco.payout.entity.Payout;
import com.innercircle.sacco.payout.entity.PayoutType;
import com.innercircle.sacco.payout.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WelfareServiceImpl implements WelfareService {

    private final WelfareBeneficiaryRepository beneficiaryRepository;
    private final WelfareBenefitCatalogRepository benefitCatalogRepository;
    private final WelfareClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final PolicyConfigResolver policyConfigResolver;
    private final JdbcTemplate jdbcTemplate;
    private final PayoutService payoutService;

    @Override
    @Transactional
    public WelfareBeneficiary createBeneficiary(CreateWelfareBeneficiaryRequest request, String actor) {
        ensureMemberExists(request.getMemberId());
        WelfareBeneficiary beneficiary = new WelfareBeneficiary();
        beneficiary.setMemberId(request.getMemberId());
        beneficiary.setFullName(request.getFullName());
        beneficiary.setRelationship(request.getRelationship());
        beneficiary.setDateOfBirth(request.getDateOfBirth());
        beneficiary.setPhone(request.getPhone());
        beneficiary.setActive(request.isActive());
        beneficiary.setNotes(request.getNotes());
        beneficiary.setCreatedBy(actor);
        return beneficiaryRepository.save(beneficiary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WelfareBeneficiary> getBeneficiariesForMember(UUID memberId) {
        return beneficiaryRepository.findByMemberId(memberId);
    }

    @Override
    @Transactional
    public WelfareClaim createClaim(CreateWelfareClaimRequest request, String actor) {
        Member member = ensureMemberExists(request.getMemberId());
        validateCoolingOff(member);

        WelfareBenefitCatalog catalog = resolveCatalog(request.getBenefitCatalogId(), request.getEventCode());
        enforceClaimLimits(request.getMemberId(), request.getEventCode(), catalog);

        WelfareClaim claim = new WelfareClaim();
        claim.setMemberId(request.getMemberId());
        claim.setBeneficiaryId(request.getBeneficiaryId());
        claim.setBenefitCatalogId(catalog != null ? catalog.getId() : request.getBenefitCatalogId());
        claim.setEventCode(request.getEventCode());
        claim.setEventDate(request.getEventDate());
        claim.setRequestedAmount(request.getRequestedAmount());
        claim.setStatus(WelfareClaimStatus.SUBMITTED);
        claim.setCreatedBy(actor);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public WelfareClaim reviewClaim(UUID claimId, ReviewWelfareClaimRequest request, String actor) {
        WelfareClaim claim = getClaim(claimId);
        if (claim.getStatus() == WelfareClaimStatus.PROCESSED) {
            throw new BusinessException("Processed claim cannot be reviewed again");
        }

        claim.setStatus(request.getStatus());
        if (request.getStatus() == WelfareClaimStatus.APPROVED) {
            claim.setApprovedAmount(request.getApprovedAmount() != null ? request.getApprovedAmount() : claim.getRequestedAmount());
        } else if (request.getStatus() == WelfareClaimStatus.REJECTED) {
            claim.setApprovedAmount(BigDecimal.ZERO);
        }
        claim.setDecisionSource(request.getDecisionSource());
        claim.setMeetingReference(request.getMeetingReference());
        claim.setDecisionDate(request.getDecisionDate() != null ? request.getDecisionDate() : LocalDate.now());
        claim.setDecisionNotes(request.getDecisionNotes());
        claim.setReviewedBy(actor);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional
    public WelfareClaim processClaim(UUID claimId, String actor, boolean isAdmin) {
        WelfareClaim claim = getClaim(claimId);
        if (claim.getStatus() != WelfareClaimStatus.APPROVED) {
            throw new BusinessException("Only approved claims can be processed");
        }
        BigDecimal amount = claim.getApprovedAmount() != null ? claim.getApprovedAmount() : claim.getRequestedAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Approved claim amount must be positive");
        }

        BigDecimal availableFund = resolveAvailableWelfareFund();
        if (availableFund.compareTo(amount) < 0) {
            throw new BusinessException("Insufficient welfare fund balance for payout");
        }

        Payout payout = payoutService.createPayout(claim.getMemberId(), amount, PayoutType.WELFARE_BENEFIT, "WELFARE_CLAIM", claim.getId(), null, "SYSTEM");
        payoutService.approvePayout(payout.getId(), actor, "Welfare claim payout", isAdmin);
        Payout processed = payoutService.processPayout(payout.getId(), actor);

        claim.setProcessedPayoutId(processed.getId());
        claim.setStatus(WelfareClaimStatus.PROCESSED);
        return claimRepository.save(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public WelfareFundSummaryResponse getFundSummary() {
        BigDecimal totalContributions = querySum(
                "SELECT COALESCE(SUM(welfare_amount), 0) FROM contributions WHERE status = 'CONFIRMED'"
        );
        BigDecimal totalPayouts = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE type = 'WELFARE_BENEFIT' AND status = 'PROCESSED'"
        );
        long pendingClaims = claimRepository.countByStatusIn(List.of(
                WelfareClaimStatus.SUBMITTED,
                WelfareClaimStatus.UNDER_REVIEW,
                WelfareClaimStatus.APPROVED
        ));

        return new WelfareFundSummaryResponse(
                totalContributions,
                totalPayouts,
                totalContributions.subtract(totalPayouts),
                pendingClaims
        );
    }

    private Member ensureMemberExists(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));
    }

    private WelfareClaim getClaim(UUID claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("WelfareClaim", claimId));
    }

    private void validateCoolingOff(Member member) {
        int coolingMonths = policyConfigResolver.requireIntAtLeast("welfare.cooling_off_months", 0);
        long monthsSinceJoin = ChronoUnit.MONTHS.between(member.getJoinDate(), LocalDate.now());
        if (monthsSinceJoin < coolingMonths) {
            throw new BusinessException("Member is still within welfare cooling-off period");
        }
    }

    private WelfareBenefitCatalog resolveCatalog(UUID benefitCatalogId, String eventCode) {
        if (benefitCatalogId != null) {
            return benefitCatalogRepository.findById(benefitCatalogId)
                    .orElseThrow(() -> new ResourceNotFoundException("WelfareBenefitCatalog", benefitCatalogId));
        }
        return benefitCatalogRepository.findByEventCode(eventCode).orElse(null);
    }

    private void enforceClaimLimits(UUID memberId, String eventCode, WelfareBenefitCatalog catalog) {
        if (catalog == null || catalog.getMaxClaimsPerYear() == null || catalog.getMaxClaimsPerYear() <= 0) {
            return;
        }

        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear());
        long existingClaims = claimRepository.countByMemberIdAndEventCodeAndStatusInAndEventDateBetween(
                memberId,
                eventCode,
                List.of(
                        WelfareClaimStatus.SUBMITTED,
                        WelfareClaimStatus.UNDER_REVIEW,
                        WelfareClaimStatus.APPROVED,
                        WelfareClaimStatus.PROCESSED
                ),
                start,
                end
        );
        if (existingClaims >= catalog.getMaxClaimsPerYear()) {
            throw new BusinessException("Member has reached annual welfare claim limit for event " + eventCode);
        }
    }

    private BigDecimal resolveAvailableWelfareFund() {
        BigDecimal accountBalance = querySum("SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE account_code = '2003'");
        if (accountBalance.compareTo(BigDecimal.ZERO) > 0) {
            return accountBalance;
        }
        BigDecimal contributions = querySum(
                "SELECT COALESCE(SUM(welfare_amount), 0) FROM contributions WHERE status = 'CONFIRMED'"
        );
        BigDecimal payouts = querySum(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE type = 'WELFARE_BENEFIT' AND status = 'PROCESSED'"
        );
        return contributions.subtract(payouts);
    }

    private BigDecimal querySum(String sql, Object... args) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        return result != null ? result : BigDecimal.ZERO;
    }
}
