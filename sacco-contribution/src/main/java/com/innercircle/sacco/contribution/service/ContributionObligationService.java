package com.innercircle.sacco.contribution.service;

import com.innercircle.sacco.contribution.entity.Contribution;
import com.innercircle.sacco.contribution.entity.ContributionObligation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface ContributionObligationService {

    List<ContributionObligation> runMonthlyObligations(YearMonth month, String actor);

    List<ContributionObligation> getObligations(YearMonth month, UUID memberId);

    UUID resolveObligationIdForContribution(UUID memberId, LocalDate contributionMonth);

    void applyConfirmedContribution(Contribution contribution);

    void reverseConfirmedContribution(Contribution contribution);
}
