package com.innercircle.sacco.member.mapper;

import com.innercircle.sacco.member.dto.CreateMemberRequest;
import com.innercircle.sacco.member.dto.MemberResponse;
import com.innercircle.sacco.member.dto.UpdateMemberRequest;
import com.innercircle.sacco.member.entity.Member;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Member toEntity(CreateMemberRequest request) {
        return new Member(
                null,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getNationalId(),
                request.getDateOfBirth(),
                request.getJoinDate()
        );
    }

    public Member toEntity(UpdateMemberRequest request) {
        Member member = new Member();
        if (request.getFirstName() != null) {
            member.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            member.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            member.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getNationalId() != null) {
            member.setNationalId(request.getNationalId());
        }
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
        }
        return member;
    }

    public MemberResponse toResponse(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .memberNumber(member.getMemberNumber())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .nationalId(member.getNationalId())
                .dateOfBirth(member.getDateOfBirth())
                .joinDate(member.getJoinDate())
                .status(member.getStatus())
                .shareBalance(member.getShareBalance())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .createdBy(member.getCreatedBy())
                .build();
    }
}
