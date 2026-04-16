package com.innercircle.sacco.common.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberConstraintValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("should accept valid E.164 phone numbers")
    void shouldAcceptValidE164PhoneNumbers() {
        TestPayload kenyaPayload = new TestPayload();
        kenyaPayload.phone = "+254712345678";

        TestPayload usPayload = new TestPayload();
        usPayload.phone = "+12025550123";

        assertThat(validator.validate(kenyaPayload)).isEmpty();
        assertThat(validator.validate(usPayload)).isEmpty();
    }

    @Test
    @DisplayName("should reject malformed phone numbers")
    void shouldRejectMalformedPhoneNumbers() {
        TestPayload payload = new TestPayload();
        payload.phone = "07123";

        assertThat(validator.validate(payload))
                .extracting(violation -> violation.getMessage())
                .contains("Phone must be a valid phone number");
    }

    private static final class TestPayload {
        @ValidPhoneNumber
        private String phone;
    }
}
