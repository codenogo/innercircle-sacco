package com.innercircle.sacco.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_withDataOnly_shouldReturnSuccessResponseWithData() {
        String data = "test-data";

        ApiResponse<String> response = ApiResponse.ok(data);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNull();
    }

    @Test
    void ok_withDataAndMessage_shouldReturnSuccessResponseWithDataAndMessage() {
        String data = "test-data";
        String message = "Operation successful";

        ApiResponse<String> response = ApiResponse.ok(data, message);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test-data");
        assertThat(response.getMessage()).isEqualTo("Operation successful");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNull();
    }

    @Test
    void ok_withNullData_shouldReturnSuccessResponseWithNullData() {
        ApiResponse<Object> response = ApiResponse.ok(null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void ok_withComplexType_shouldReturnSuccessResponse() {
        Integer data = 42;

        ApiResponse<Integer> response = ApiResponse.ok(data);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(42);
    }

    @Test
    void error_withMessageOnly_shouldReturnFailureResponseWithMessage() {
        String message = "Something went wrong";

        ApiResponse<Object> response = ApiResponse.error(message);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getPath()).isNull();
    }

    @Test
    void error_withMessageAndPath_shouldReturnFailureResponseWithMessageAndPath() {
        String message = "Resource not found";
        String path = "/api/v1/members/123";

        ApiResponse<Object> response = ApiResponse.error(message, path);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getMessage()).isEqualTo("Resource not found");
        assertThat(response.getPath()).isEqualTo("/api/v1/members/123");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void error_withNullMessage_shouldReturnFailureResponseWithNullMessage() {
        ApiResponse<Object> response = ApiResponse.error(null);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isNull();
    }

    @Test
    void builder_shouldCreateResponseWithAllFields() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .data("built-data")
                .message("built-message")
                .path("/api/test")
                .build();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("built-data");
        assertThat(response.getMessage()).isEqualTo("built-message");
        assertThat(response.getPath()).isEqualTo("/api/test");
    }

    @Test
    void constructor_shouldCreateResponseWithAllFields() {
        java.time.Instant now = java.time.Instant.now();

        ApiResponse<String> response = new ApiResponse<>(true, "data", "msg", now, "/path");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getMessage()).isEqualTo("msg");
        assertThat(response.getTimestamp()).isEqualTo(now);
        assertThat(response.getPath()).isEqualTo("/path");
    }
}
