package com.innercircle.sacco.member.service;

import com.innercircle.sacco.common.dto.CursorPage;
import com.innercircle.sacco.common.event.MemberCreatedEvent;
import com.innercircle.sacco.common.exception.BusinessException;
import com.innercircle.sacco.common.exception.ResourceNotFoundException;
import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import com.innercircle.sacco.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Member create(Member member) {
        // Validate uniqueness
        if (memberRepository.existsByMemberNumber(member.getMemberNumber())) {
            throw new BusinessException("Member number already exists: " + member.getMemberNumber());
        }
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new BusinessException("Email already exists: " + member.getEmail());
        }
        if (memberRepository.existsByNationalId(member.getNationalId())) {
            throw new BusinessException("National ID already exists: " + member.getNationalId());
        }

        Member savedMember = memberRepository.save(member);

        eventPublisher.publishEvent(new MemberCreatedEvent(
                savedMember.getId(),
                savedMember.getMemberNumber(),
                savedMember.getFirstName(),
                savedMember.getLastName(),
                getCurrentActor()));

        return savedMember;
    }

    @Override
    @Transactional
    public Member update(UUID id, Member member) {
        Member existing = findById(id);

        // Update fields only if provided (not null)
        if (member.getFirstName() != null) {
            existing.setFirstName(member.getFirstName());
        }
        if (member.getLastName() != null) {
            existing.setLastName(member.getLastName());
        }
        if (member.getEmail() != null) {
            // Validate email uniqueness if changed
            if (!existing.getEmail().equals(member.getEmail())
                    && memberRepository.existsByEmail(member.getEmail())) {
                throw new BusinessException("Email already exists: " + member.getEmail());
            }
            existing.setEmail(member.getEmail());
        }
        if (member.getPhone() != null) {
            existing.setPhone(member.getPhone());
        }
        if (member.getNationalId() != null) {
            // Validate national ID uniqueness if changed
            if (!existing.getNationalId().equals(member.getNationalId())
                    && memberRepository.existsByNationalId(member.getNationalId())) {
                throw new BusinessException("National ID already exists: " + member.getNationalId());
            }
            existing.setNationalId(member.getNationalId());
        }
        if (member.getDateOfBirth() != null) {
            existing.setDateOfBirth(member.getDateOfBirth());
        }

        Member updatedMember = memberRepository.save(existing);

        // TODO: Publish MemberUpdatedEvent when event is defined

        return updatedMember;
    }

    @Override
    @Transactional(readOnly = true)
    public Member findById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Member findByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<Member> list(String cursor, int size) {
        UUID cursorId = (cursor != null && !cursor.isEmpty())
                ? UUID.fromString(cursor)
                : new UUID(0L, 0L);

        List<Member> members = memberRepository.findByIdGreaterThanOrderById(
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasMore = members.size() > size;
        if (hasMore) {
            members = members.subList(0, size);
        }

        String nextCursor = hasMore && !members.isEmpty()
                ? members.get(members.size() - 1).getId().toString()
                : null;

        return CursorPage.of(members, nextCursor, hasMore);
    }

    @Override
    @Transactional
    public Member suspend(UUID id) {
        Member member = findById(id);

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            throw new BusinessException("Member is already suspended");
        }

        member.setStatus(MemberStatus.SUSPENDED);
        Member suspendedMember = memberRepository.save(member);

        // TODO: Publish MemberSuspendedEvent when event is defined

        return suspendedMember;
    }

    @Override
    @Transactional
    public Member reactivate(UUID id) {
        Member member = findById(id);

        if (member.getStatus() == MemberStatus.ACTIVE) {
            throw new BusinessException("Member is already active");
        }

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new BusinessException("Cannot reactivate a deactivated member");
        }

        member.setStatus(MemberStatus.ACTIVE);
        Member reactivatedMember = memberRepository.save(member);

        // TODO: Publish MemberReactivatedEvent when event is defined

        return reactivatedMember;
    }

    private String getCurrentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
