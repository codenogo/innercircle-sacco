package com.innercircle.sacco.ledger.entity;

public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE;

    /**
     * Returns true for account types that increase on debit (ASSET, EXPENSE).
     * Returns false for account types that increase on credit (LIABILITY, EQUITY, REVENUE).
     */
    public boolean isNormalDebit() {
        return this == ASSET || this == EXPENSE;
    }
}
