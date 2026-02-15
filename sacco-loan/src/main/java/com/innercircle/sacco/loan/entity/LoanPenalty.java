package com.innercircle.sacco.loan.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanPenalty extends BaseEntity {

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false)
    private Boolean applied = false;

    private Instant appliedAt;

    @Column(nullable = false)
    private Boolean paid = false;

    private Instant paidAt;

    private UUID scheduleId;
}
