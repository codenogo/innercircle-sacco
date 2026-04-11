package com.innercircle.sacco.common.reference;

public enum ReferenceSeries {
    PAYOUT("PAY", "payout_reference_seq", 10),
    PETTY_CASH("PC", "petty_cash_reference_seq", 10),
    INVESTMENT("INV", "investment_reference_seq", 10),
    LOAN_NUMBER("LN", "loan_number_seq", 10);

    private final String prefix;
    private final String sequenceName;
    private final int width;

    ReferenceSeries(String prefix, String sequenceName, int width) {
        this.prefix = prefix;
        this.sequenceName = sequenceName;
        this.width = width;
    }

    public String prefix() {
        return prefix;
    }

    public String sequenceName() {
        return sequenceName;
    }

    public int width() {
        return width;
    }
}
