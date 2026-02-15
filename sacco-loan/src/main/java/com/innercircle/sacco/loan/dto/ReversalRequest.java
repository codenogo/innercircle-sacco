package com.innercircle.sacco.loan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReversalRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotBlank(message = "Actor is required")
    private String actor;
}
