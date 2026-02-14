package com.innercircle.sacco.member.controller;

import com.innercircle.sacco.common.dto.ApiResponse;
import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.member.dto.CreateMemberRequest;
import com.innercircle.sacco.member.dto.MemberResponse;
import com.innercircle.sacco.member.dto.UpdateMemberRequest;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.mapper.MemberMapper;
import com.innercircle.sacco.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> create(@Valid @RequestBody CreateMemberRequest request) {
        Member member = memberMapper.toEntity(request);
        Member created = memberService.create(member);
        return ApiResponse.ok(memberMapper.toResponse(created), "Member created successfully");
    }

    @GetMapping
    public ApiResponse<CursorPage<MemberResponse>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        CursorPage<Member> page = memberService.list(cursor, size);
        CursorPage<MemberResponse> responsePage = CursorPage.of(
                page.getItems().stream()
                        .map(memberMapper::toResponse)
                        .toList(),
                page.getNextCursor(),
                page.isHasMore()
        );
        return ApiResponse.ok(responsePage);
    }

    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable UUID id) {
        Member member = memberService.findById(id);
        return ApiResponse.ok(memberMapper.toResponse(member));
    }

    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMemberRequest request) {

        Member updateData = memberMapper.toEntity(request);
        Member updated = memberService.update(id, updateData);
        return ApiResponse.ok(memberMapper.toResponse(updated), "Member updated successfully");
    }

    @PatchMapping("/{id}/suspend")
    public ApiResponse<MemberResponse> suspend(@PathVariable UUID id) {
        Member suspended = memberService.suspend(id);
        return ApiResponse.ok(memberMapper.toResponse(suspended), "Member suspended successfully");
    }

    @PatchMapping("/{id}/reactivate")
    public ApiResponse<MemberResponse> reactivate(@PathVariable UUID id) {
        Member reactivated = memberService.reactivate(id);
        return ApiResponse.ok(memberMapper.toResponse(reactivated), "Member reactivated successfully");
    }
}
