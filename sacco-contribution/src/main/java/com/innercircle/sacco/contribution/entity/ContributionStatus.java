package com.innercircle.sacco.contribution.entity;

/**
 * Status of a contribution.
 * PENDING - Recorded but not yet confirmed by treasurer
 * CONFIRMED - Confirmed and will be posted to ledger
 * REVERSED - Reversed (e.g., due to payment failure)
 */
public enum ContributionStatus {
    PENDING,
    CONFIRMED,
    REVERSED
}
