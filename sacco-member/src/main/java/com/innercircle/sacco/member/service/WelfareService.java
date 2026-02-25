package com.innercircle.sacco.member.service;

import com.innercircle.sacco.member.dto.CreateWelfareBeneficiaryRequest;
import com.innercircle.sacco.member.dto.CreateWelfareClaimRequest;
import com.innercircle.sacco.member.dto.ReviewWelfareClaimRequest;
import com.innercircle.sacco.member.dto.WelfareFundSummaryResponse;
import com.innercircle.sacco.member.entity.WelfareBeneficiary;
import com.innercircle.sacco.member.entity.WelfareClaim;

import java.util.List;
import java.util.UUID;

public interface WelfareService {
    WelfareBeneficiary createBeneficiary(CreateWelfareBeneficiaryRequest request, String actor);
    List<WelfareBeneficiary> getBeneficiariesForMember(UUID memberId);
    WelfareClaim createClaim(CreateWelfareClaimRequest request, String actor);
    WelfareClaim reviewClaim(UUID claimId, ReviewWelfareClaimRequest request, String actor);
    WelfareClaim processClaim(UUID claimId, String actor, boolean isAdmin);
    WelfareFundSummaryResponse getFundSummary();
}
