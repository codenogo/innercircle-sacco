package com.innercircle.sacco.contribution.entity;

/**
 * Type of contribution made by a member.
 * REGULAR - Recurring monthly or scheduled contributions
 * SPECIAL - One-off special contributions
 * PENALTY_PAYMENT - Payment towards a penalty
 */
public enum ContributionType {
    REGULAR,
    SPECIAL,
    PENALTY_PAYMENT
}
