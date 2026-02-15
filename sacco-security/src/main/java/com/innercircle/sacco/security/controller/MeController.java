package com.innercircle.sacco.security.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.repository.MemberRepository;
import com.innercircle.sacco.security.dto.MeResponse;
import com.innercircle.sacco.security.dto.MemberSummary;
import com.innercircle.sacco.security.entity.Role;
import com.innercircle.sacco.security.entity.UserAccount;
import com.innercircle.sacco.security.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<MeResponse>> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserAccount user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        MemberSummary memberSummary = null;
        if (user.getMemberId() != null) {
            Member member = memberRepository.findById(user.getMemberId()).orElse(null);
            if (member != null) {
                memberSummary = new MemberSummary(
                    member.getId(),
                    member.getFirstName(),
                    member.getLastName(),
                    member.getMemberNumber()
                );
            }
        }

        MeResponse response = new MeResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getEnabled(),
            user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()),
            memberSummary,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
