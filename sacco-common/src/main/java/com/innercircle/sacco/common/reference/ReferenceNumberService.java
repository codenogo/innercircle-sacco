package com.innercircle.sacco.common.reference;

import org.springframework.stereotype.Service;

@Service
public class ReferenceNumberService {

    private final ReferenceSequenceRepository sequenceRepository;

    public ReferenceNumberService(ReferenceSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    public String nextReference(ReferenceSeries series) {
        long sequenceValue = switch (series) {
            case PAYOUT -> sequenceRepository.nextPayoutReference();
            case PETTY_CASH -> sequenceRepository.nextPettyCashReference();
            case INVESTMENT -> sequenceRepository.nextInvestmentReference();
            case LOAN_NUMBER -> sequenceRepository.nextLoanNumber();
        };
        return format(series, sequenceValue);
    }

    String format(ReferenceSeries series, long sequenceValue) {
        if (sequenceValue < 0) {
            throw new IllegalArgumentException("Sequence value cannot be negative");
        }
        return series.prefix() + "-" + String.format("%0" + series.width() + "d", sequenceValue);
    }
}
