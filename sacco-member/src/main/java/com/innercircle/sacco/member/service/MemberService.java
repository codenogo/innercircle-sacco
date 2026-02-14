package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.member.entity.Member;

import java.util.UUID;

public interface MemberService {

    Member create(Member member);

    Member update(UUID id, Member member);

    Member findById(UUID id);

    Member findByMemberNumber(String memberNumber);

    CursorPage<Member> list(String cursor, int size);

    Member suspend(UUID id);

    Member reactivate(UUID id);
}
