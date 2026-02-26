package com.innercircle.sacco.investment.repository;

import com.innercircle.sacco.investment.entity.InvestmentValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentValuationRepository extends JpaRepository<InvestmentValuation, UUID> {

    List<InvestmentValuation> findByInvestmentIdOrderByValuationDateDescCreatedAtDesc(UUID investmentId);
}
