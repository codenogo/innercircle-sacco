package com.innercircle.sacco.loan.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "loan_guarantors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanGuarantor extends BaseEntity {

    @Column(nullable = false)
    private UUID loanId;

    @Column(nullable = false)
    private UUID guarantorMemberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal guaranteedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuarantorStatus status = GuarantorStatus.PENDING;
}
