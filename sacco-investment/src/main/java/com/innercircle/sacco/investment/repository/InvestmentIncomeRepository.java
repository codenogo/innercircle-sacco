package com.innercircle.sacco.investment.repository;

import com.innercircle.sacco.investment.entity.InvestmentIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvestmentIncomeRepository extends JpaRepository<InvestmentIncome, UUID> {

    List<InvestmentIncome> findByInvestmentIdOrderByIncomeDateDescCreatedAtDesc(UUID investmentId);

    @Query("""
            SELECT COALESCE(SUM(i.amount), 0)
            FROM InvestmentIncome i
            WHERE i.incomeDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumByIncomeDateBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
