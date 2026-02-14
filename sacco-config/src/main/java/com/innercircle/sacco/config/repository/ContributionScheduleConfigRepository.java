package com.innercircle.sacco.config.repository;

import com.innercircle.sacco.config.entity.ContributionScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContributionScheduleConfigRepository extends JpaRepository<ContributionScheduleConfig, UUID> {
    List<ContributionScheduleConfig> findByActiveTrue();
    List<ContributionScheduleConfig> findByActiveTrueOrderByNameAsc();
}
