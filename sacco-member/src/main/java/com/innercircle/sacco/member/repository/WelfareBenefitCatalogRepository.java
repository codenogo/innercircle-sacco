package com.innercircle.sacco.member.repository;

import com.innercircle.sacco.member.entity.WelfareBenefitCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WelfareBenefitCatalogRepository extends JpaRepository<WelfareBenefitCatalog, UUID> {
    Optional<WelfareBenefitCatalog> findByEventCode(String eventCode);
}
