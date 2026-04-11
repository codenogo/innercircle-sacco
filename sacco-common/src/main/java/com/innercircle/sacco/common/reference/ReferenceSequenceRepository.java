package com.innercircle.sacco.common.reference;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReferenceSequenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReferenceSequenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextPayoutReference() {
        return querySingle("SELECT nextval('payout_reference_seq')", "payout_reference_seq");
    }

    public long nextPettyCashReference() {
        return querySingle("SELECT nextval('petty_cash_reference_seq')", "petty_cash_reference_seq");
    }

    public long nextInvestmentReference() {
        return querySingle("SELECT nextval('investment_reference_seq')", "investment_reference_seq");
    }

    public long nextLoanNumber() {
        return querySingle("SELECT nextval('loan_number_seq')", "loan_number_seq");
    }

    private long querySingle(String sql, String sequenceName) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        if (value == null) {
            throw new IllegalStateException("Sequence returned null value for " + sequenceName);
        }
        return value;
    }
}
