package com.innercircle.sacco.investment.repository;

import com.innercircle.sacco.investment.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, UUID> {

    boolean existsByReferenceNumber(String referenceNumber);

    List<Investment> findAllByOrderByCreatedAtDesc();
}
