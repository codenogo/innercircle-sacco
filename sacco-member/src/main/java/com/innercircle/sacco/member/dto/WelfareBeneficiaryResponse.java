package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.WelfareBeneficiary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WelfareBeneficiaryResponse(
        UUID id,
        UUID memberId,
        String fullName,
        String relationship,
        LocalDate dateOfBirth,
        String phone,
        boolean active,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static WelfareBeneficiaryResponse fromEntity(WelfareBeneficiary beneficiary) {
        return new WelfareBeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getMemberId(),
                beneficiary.getFullName(),
                beneficiary.getRelationship(),
                beneficiary.getDateOfBirth(),
                beneficiary.getPhone(),
                beneficiary.isActive(),
                beneficiary.getNotes(),
                beneficiary.getCreatedAt(),
                beneficiary.getUpdatedAt()
        );
    }
}
