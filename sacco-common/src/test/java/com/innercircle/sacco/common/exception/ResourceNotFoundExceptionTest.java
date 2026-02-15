package com.innercircle.sacco.common.exception;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_shouldFormatMessageWithResourceNameAndIdentifier() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Member", 123);

        assertThat(exception.getMessage()).isEqualTo("Member not found with identifier: 123");
    }

    @Test
    void constructor_withStringIdentifier_shouldFormatMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Loan", "LOAN-001");

        assertThat(exception.getMessage()).isEqualTo("Loan not found with identifier: LOAN-001");
    }

    @Test
    void constructor_withUuidIdentifier_shouldFormatMessage() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        ResourceNotFoundException exception = new ResourceNotFoundException("Account", uuid);

        assertThat(exception.getMessage())
                .isEqualTo("Account not found with identifier: 550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void shouldExtendBusinessException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource", 1);

        assertThat(exception).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldExtendRuntimeException() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource", 1);

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeThrowable() {
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("Member", 456);
        })
                .isInstanceOf(ResourceNotFoundException.class)
                .isInstanceOf(BusinessException.class)
                .hasMessage("Member not found with identifier: 456");
    }

    @Test
    void constructor_withNullIdentifier_shouldFormatWithNull() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Entity", null);

        assertThat(exception.getMessage()).isEqualTo("Entity not found with identifier: null");
    }

    @Test
    void constructor_withLongIdentifier_shouldFormatMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Transaction", 99999999L);

        assertThat(exception.getMessage()).isEqualTo("Transaction not found with identifier: 99999999");
    }

    @Test
    void getCause_shouldReturnNull() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Item", 1);

        assertThat(exception.getCause()).isNull();
    }
}
