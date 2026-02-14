package com.innercircle.sacco.loan.dto;

import com.innercircle.sacco.loan.entity.RepaymentSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class RepaymentScheduleResponse {

    private UUID id;
    private UUID loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private Boolean paid;

    public static RepaymentScheduleResponse from(RepaymentSchedule schedule) {
        return RepaymentScheduleResponse.builder()
                .id(schedule.getId())
                .loanId(schedule.getLoanId())
                .installmentNumber(schedule.getInstallmentNumber())
                .dueDate(schedule.getDueDate())
                .principalAmount(schedule.getPrincipalAmount())
                .interestAmount(schedule.getInterestAmount())
                .totalAmount(schedule.getTotalAmount())
                .paid(schedule.getPaid())
                .build();
    }
}
