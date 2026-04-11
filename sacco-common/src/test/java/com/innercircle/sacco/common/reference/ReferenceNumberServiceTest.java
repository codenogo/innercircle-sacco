package com.innercircle.sacco.common.reference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceNumberServiceTest {

    @Mock
    private ReferenceSequenceRepository sequenceRepository;

    @Test
    void nextReference_shouldFormatPayoutWithZeroPadding() {
        when(sequenceRepository.nextPayoutReference()).thenReturn(1L);
        ReferenceNumberService service = new ReferenceNumberService(sequenceRepository);

        String reference = service.nextReference(ReferenceSeries.PAYOUT);

        assertThat(reference).isEqualTo("PAY-0000000001");
    }

    @Test
    void nextReference_shouldFormatAllSeries() {
        when(sequenceRepository.nextPayoutReference()).thenReturn(12L);
        when(sequenceRepository.nextPettyCashReference()).thenReturn(34L);
        when(sequenceRepository.nextInvestmentReference()).thenReturn(56L);
        when(sequenceRepository.nextLoanNumber()).thenReturn(78L);
        ReferenceNumberService service = new ReferenceNumberService(sequenceRepository);

        assertThat(service.nextReference(ReferenceSeries.PAYOUT)).isEqualTo("PAY-0000000012");
        assertThat(service.nextReference(ReferenceSeries.PETTY_CASH)).isEqualTo("PC-0000000034");
        assertThat(service.nextReference(ReferenceSeries.INVESTMENT)).isEqualTo("INV-0000000056");
        assertThat(service.nextReference(ReferenceSeries.LOAN_NUMBER)).isEqualTo("LN-0000000078");
    }

    @Test
    void nextReference_shouldProduceDistinctValuesAcrossCalls() {
        when(sequenceRepository.nextPayoutReference()).thenReturn(100L, 101L);
        ReferenceNumberService service = new ReferenceNumberService(sequenceRepository);

        String first = service.nextReference(ReferenceSeries.PAYOUT);
        String second = service.nextReference(ReferenceSeries.PAYOUT);

        assertThat(first).isEqualTo("PAY-0000000100");
        assertThat(second).isEqualTo("PAY-0000000101");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void nextReference_shouldRejectNegativeSequenceValue() {
        when(sequenceRepository.nextInvestmentReference()).thenReturn(-1L);
        ReferenceNumberService service = new ReferenceNumberService(sequenceRepository);

        assertThatThrownBy(() -> service.nextReference(ReferenceSeries.INVESTMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }
}
