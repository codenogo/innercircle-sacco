package com.innercircle.sacco.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessExceptionTest {

    @Test
    void constructor_withMessage_shouldSetMessage() {
        BusinessException exception = new BusinessException("Insufficient funds");

        assertThat(exception.getMessage()).isEqualTo("Insufficient funds");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_shouldSetBoth() {
        Throwable cause = new IllegalStateException("Root cause");

        BusinessException exception = new BusinessException("Operation failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Operation failed");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldExtendRuntimeException() {
        BusinessException exception = new BusinessException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldBeThrowable() {
        assertThatThrownBy(() -> {
            throw new BusinessException("Business rule violated");
        })
                .isInstanceOf(BusinessException.class)
                .hasMessage("Business rule violated");
    }

    @Test
    void shouldBeThrowableWithCause() {
        RuntimeException cause = new RuntimeException("underlying issue");

        assertThatThrownBy(() -> {
            throw new BusinessException("Wrapper message", cause);
        })
                .isInstanceOf(BusinessException.class)
                .hasMessage("Wrapper message")
                .hasCause(cause);
    }

    @Test
    void constructor_withNullMessage_shouldAcceptNull() {
        BusinessException exception = new BusinessException(null);

        assertThat(exception.getMessage()).isNull();
    }

    @Test
    void constructor_withEmptyMessage_shouldAcceptEmpty() {
        BusinessException exception = new BusinessException("");

        assertThat(exception.getMessage()).isEmpty();
    }

    @Test
    void constructor_withNullCause_shouldAcceptNull() {
        BusinessException exception = new BusinessException("msg", null);

        assertThat(exception.getMessage()).isEqualTo("msg");
        assertThat(exception.getCause()).isNull();
    }
}
