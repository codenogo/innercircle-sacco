package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.WelfareBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WelfareBeneficiaryRepository extends JpaRepository<WelfareBeneficiary, UUID> {
    List<WelfareBeneficiary> findByMemberId(UUID memberId);
}
