package com.innercircle.sacco.loan.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchProcessingStatusTest {

    @Test
    @DisplayName("should have all three values: STARTED, COMPLETED, FAILED")
    void shouldHaveAllThreeValues() {
        BatchProcessingStatus[] values = BatchProcessingStatus.values();

        assertThat(values).hasSize(3);
        assertThat(values).containsExactlyInAnyOrder(
                BatchProcessingStatus.STARTED,
                BatchProcessingStatus.COMPLETED,
                BatchProcessingStatus.FAILED
        );
    }

    @Test
    @DisplayName("should return STARTED when valueOf is called with 'STARTED'")
    void shouldReturnStartedFromValueOf() {
        BatchProcessingStatus status = BatchProcessingStatus.valueOf("STARTED");

        assertThat(status).isEqualTo(BatchProcessingStatus.STARTED);
    }

    @Test
    @DisplayName("should return COMPLETED when valueOf is called with 'COMPLETED'")
    void shouldReturnCompletedFromValueOf() {
        BatchProcessingStatus status = BatchProcessingStatus.valueOf("COMPLETED");

        assertThat(status).isEqualTo(BatchProcessingStatus.COMPLETED);
    }

    @Test
    @DisplayName("should return FAILED when valueOf is called with 'FAILED'")
    void shouldReturnFailedFromValueOf() {
        BatchProcessingStatus status = BatchProcessingStatus.valueOf("FAILED");

        assertThat(status).isEqualTo(BatchProcessingStatus.FAILED);
    }

    @Test
    @DisplayName("values() should return array of length 3")
    void valuesShouldReturnArrayOfLength3() {
        BatchProcessingStatus[] values = BatchProcessingStatus.values();

        assertThat(values).hasSize(3);
    }

    @Test
    @DisplayName("should have correct enum constant names")
    void shouldHaveCorrectEnumConstantNames() {
        assertThat(BatchProcessingStatus.STARTED.name()).isEqualTo("STARTED");
        assertThat(BatchProcessingStatus.COMPLETED.name()).isEqualTo("COMPLETED");
        assertThat(BatchProcessingStatus.FAILED.name()).isEqualTo("FAILED");
    }
}
