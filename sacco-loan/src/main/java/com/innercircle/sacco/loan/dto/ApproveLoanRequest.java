package com.innercircle.sacco.loan.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveLoanRequest {

    @Size(max = 500, message = "Override reason must not exceed 500 characters")
    private String overrideReason;
}
