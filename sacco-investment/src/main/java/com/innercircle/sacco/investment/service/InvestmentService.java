package com.innercircle.sacco.investment.service;

import com.innercircle.sacco.investment.dto.CreateInvestmentRequest;
import com.innercircle.sacco.investment.dto.DisposeInvestmentRequest;
import com.innercircle.sacco.investment.dto.InvestmentSummaryResponse;
import com.innercircle.sacco.investment.dto.RecordIncomeRequest;
import com.innercircle.sacco.investment.dto.RecordValuationRequest;
import com.innercircle.sacco.investment.dto.RollOverRequest;
import com.innercircle.sacco.investment.entity.Investment;
import com.innercircle.sacco.investment.entity.InvestmentIncome;
import com.innercircle.sacco.investment.entity.InvestmentValuation;

import java.util.List;
import java.util.UUID;

public interface InvestmentService {

    List<Investment> listInvestments();

    Investment getInvestment(UUID investmentId);

    InvestmentSummaryResponse getSummary();

    Investment createInvestment(CreateInvestmentRequest request, String actor);

    Investment approveInvestment(UUID investmentId, String actor);

    Investment rejectInvestment(UUID investmentId, String reason, String actor);

    Investment activateInvestment(UUID investmentId, String actor);

    Investment disposeInvestment(UUID investmentId, DisposeInvestmentRequest request, String actor);

    Investment rollOverInvestment(UUID investmentId, RollOverRequest request, String actor);

    List<InvestmentIncome> getIncomeHistory(UUID investmentId);

    InvestmentIncome recordIncome(UUID investmentId, RecordIncomeRequest request, String actor);

    List<InvestmentValuation> getValuationHistory(UUID investmentId);

    InvestmentValuation recordValuation(UUID investmentId, RecordValuationRequest request, String actor);
}
