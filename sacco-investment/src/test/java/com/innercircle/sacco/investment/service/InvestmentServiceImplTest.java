package com.innercircle.sacco.investment.service;

import com.innercircle.sacco.common.outbox.EventOutboxWriter;
import com.innercircle.sacco.investment.dto.CreateInvestmentRequest;
import com.innercircle.sacco.investment.entity.Investment;
import com.innercircle.sacco.investment.entity.InvestmentType;
import com.innercircle.sacco.investment.repository.InvestmentIncomeRepository;
import com.innercircle.sacco.investment.repository.InvestmentRepository;
import com.innercircle.sacco.investment.repository.InvestmentValuationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceImplTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private InvestmentIncomeRepository investmentIncomeRepository;
    @Mock
    private InvestmentValuationRepository investmentValuationRepository;
    @Mock
    private EventOutboxWriter outboxWriter;

    @InjectMocks
    private InvestmentServiceImpl investmentService;

    @Captor
    private ArgumentCaptor<Investment> investmentCaptor;

    @Test
    void createInvestment_shouldGenerateReferenceWithInvPrefix() {
        when(investmentRepository.existsByReferenceNumber(any())).thenReturn(false);
        when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
            Investment investment = invocation.getArgument(0);
            investment.setId(UUID.randomUUID());
            return investment;
        });

        investmentService.createInvestment(createRequest(), "maker.user");

        verify(investmentRepository).save(investmentCaptor.capture());
        assertThat(investmentCaptor.getValue().getReferenceNumber()).startsWith("INV-");
    }

    @Test
    void createInvestment_shouldPersistInvestmentDetails() {
        when(investmentRepository.existsByReferenceNumber(any())).thenReturn(false);
        when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
            Investment investment = invocation.getArgument(0);
            investment.setId(UUID.randomUUID());
            return investment;
        });

        Investment result = investmentService.createInvestment(createRequest(), "maker.user");

        assertThat(result.getName()).isEqualTo("Treasury Bill 364D");
        assertThat(result.getInvestmentType()).isEqualTo(InvestmentType.TREASURY_BILL);
        assertThat(result.getPurchasePrice()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    private CreateInvestmentRequest createRequest() {
        return new CreateInvestmentRequest(
                "Treasury Bill 364D",
                InvestmentType.TREASURY_BILL,
                "National Treasury",
                new BigDecimal("100000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("12.50"),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 12, 15),
                null,
                null,
                "Initial purchase"
        );
    }
}
