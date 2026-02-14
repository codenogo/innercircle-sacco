package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.Member;
import com.innercircle.sacco.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByMemberNumber(String memberNumber);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByNationalId(String nationalId);

    List<Member> findByStatus(MemberStatus status);

    List<Member> findByIdGreaterThanOrderById(UUID cursor, org.springframework.data.domain.Pageable pageable);

    boolean existsByMemberNumber(String memberNumber);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);
}
