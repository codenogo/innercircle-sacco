package com.innercircle.sacco.config.repository;

import com.innercircle.sacco.config.entity.LoanProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanProductConfigRepository extends JpaRepository<LoanProductConfig, UUID> {
    List<LoanProductConfig> findByActiveTrue();
    List<LoanProductConfig> findByActiveTrueOrderByNameAsc();
}
