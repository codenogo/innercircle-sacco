package com.innercircle.sacco.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateWelfareBeneficiaryRequest {
    @NotNull
    private UUID memberId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String relationship;

    private LocalDate dateOfBirth;
    private String phone;
    private boolean active = true;
    private String notes;
}
