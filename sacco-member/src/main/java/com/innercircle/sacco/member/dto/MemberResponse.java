package com.innercircle.sacco.member.dto;

import com.innercircle.sacco.member.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private UUID id;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String nationalId;
    private LocalDate dateOfBirth;
    private LocalDate joinDate;
    private MemberStatus status;
    private BigDecimal shareBalance;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
