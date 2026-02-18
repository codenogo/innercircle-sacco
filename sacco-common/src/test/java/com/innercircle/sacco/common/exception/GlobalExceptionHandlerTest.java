package com.innercircle.sacco.common.exception;

import com.innercircle.sacco.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    // --- ResourceNotFoundException handling ---

    @Test
    void handleNotFound_shouldReturnNotFoundStatus() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Member", 123);

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleNotFound_shouldReturnErrorBody() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Member", 123);

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex, request);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).isEqualTo("Member not found with identifier: 123");
        assertThat(body.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleNotFound_shouldIncludeRequestPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/members/999");
        ResourceNotFoundException ex = new ResourceNotFoundException("Account", "ACC-001");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/members/999");
    }

    // --- InvalidStateTransitionException handling ---

    @Test
    void handleInvalidStateTransition_shouldReturnConflictStatus() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Loan", "PENDING", "CLOSED");

        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidStateTransition(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalidStateTransition_shouldReturnErrorBody() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Member", "ACTIVE", "ACTIVE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidStateTransition(ex, request);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).contains("Member");
        assertThat(body.getMessage()).contains("ACTIVE");
        assertThat(body.getPath()).isEqualTo("/api/v1/test");
    }

    // --- BusinessException handling ---

    @Test
    void handleBusiness_shouldReturnBadRequestStatus() {
        BusinessException ex = new BusinessException("Insufficient balance");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleBusiness_shouldReturnErrorBody() {
        BusinessException ex = new BusinessException("Insufficient balance");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex, request);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).isEqualTo("Insufficient balance");
        assertThat(body.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleBusiness_shouldIncludeRequestPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/loans/apply");
        BusinessException ex = new BusinessException("Loan limit exceeded");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/loans/apply");
    }

    // --- MethodArgumentNotValidException handling ---

    @Test
    void handleValidation_shouldReturnBadRequestStatus() {
        MethodArgumentNotValidException ex = mockValidationException(
                List.of(new FieldError("obj", "email", "must not be blank"))
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidation_shouldReturnConcatenatedFieldErrors() {
        MethodArgumentNotValidException ex = mockValidationException(
                List.of(
                        new FieldError("obj", "email", "must not be blank"),
                        new FieldError("obj", "name", "size must be between 1 and 100")
                )
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).contains("email: must not be blank");
        assertThat(body.getMessage()).contains("name: size must be between 1 and 100");
        assertThat(body.getMessage()).contains(", ");
    }

    @Test
    void handleValidation_withSingleFieldError_shouldReturnSingleError() {
        MethodArgumentNotValidException ex = mockValidationException(
                List.of(new FieldError("obj", "amount", "must be positive"))
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        assertThat(response.getBody().getMessage()).isEqualTo("amount: must be positive");
    }

    @Test
    void handleValidation_shouldIncludeRequestPath() {
        MethodArgumentNotValidException ex = mockValidationException(
                List.of(new FieldError("obj", "field", "invalid"))
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
    }

    // --- General Exception handling ---

    @Test
    void handleGeneral_shouldReturnInternalServerErrorStatus() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleGeneral_shouldReturnGenericErrorMessage() {
        Exception ex = new RuntimeException("Database connection lost");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex, request);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(body.getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleGeneral_shouldNotLeakExceptionDetails() {
        Exception ex = new RuntimeException("Sensitive SQL error: SELECT * FROM users WHERE password='admin'");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex, request);

        assertThat(response.getBody().getMessage()).doesNotContain("SQL");
        assertThat(response.getBody().getMessage()).doesNotContain("password");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleGeneral_withNullPointerException_shouldReturnInternalServerError() {
        Exception ex = new NullPointerException("null reference");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    // --- Helper ---

    private MethodArgumentNotValidException mockValidationException(List<FieldError> fieldErrors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        return ex;
    }
}
