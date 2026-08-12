package com.neopick.adapter.web.exception;

import com.neopick.adapter.web.dto.common.ErrorResponse;
import com.neopick.domain.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("BusinessException → 400")
    class HandleBusinessException {

        @Test
        @DisplayName("should return 400 with error code")
        void shouldReturnBadRequest() {
            ResponseEntity<ErrorResponse> resp = handler.handleBusiness(
                    new com.neopick.domain.common.BusinessException("BOOKING_NOT_FOUND", "Booking does not exist"));

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody().message()).contains("Booking does not exist");
            assertThat(resp.getBody().errorCode()).isEqualTo("BOOKING_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException → 400")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return 400 with INVALID_ARGUMENT code")
        void shouldReturnBadRequest() {
            ResponseEntity<ErrorResponse> resp = handler.handleIllegalArgument(
                    new java.lang.IllegalArgumentException("Invalid phone number"));

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody().errorCode()).isEqualTo("INVALID_ARGUMENT");
            assertThat(resp.getBody().message()).isEqualTo("Invalid phone number");
        }
    }

    @Nested
    @DisplayName("IllegalStateException → 409")
    class HandleIllegalState {

        @Test
        @DisplayName("should return 409 CONFLICT")
        void shouldReturnConflict() {
            ResponseEntity<ErrorResponse> resp = handler.handleIllegalState(
                    new java.lang.IllegalStateException("Already favorited"));

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody().errorCode()).isEqualTo("CONFLICT");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException → 400")
    class ValidationException {

        @Test
        @DisplayName("should return 400 with VALIDATION_ERROR code")
        void shouldReturnValidationError() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "target");
            bindingResult.addError(new FieldError("target", "phone",
                    "must not be blank"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody().errorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(resp.getBody().message()).contains("must not be blank");
        }
    }

    @Nested
    @DisplayName("Generic Exception → 500")
    class GenericException {

        @Test
        @DisplayName("should return 500 INTERNAL_ERROR for unexpected exceptions")
        void shouldReturnInternalError() {
            ResponseEntity<ErrorResponse> resp = handler.handleGeneral(
                    new RuntimeException("Something broke"));

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(resp.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(resp.getBody().message()).isEqualTo("Internal server error");
        }
    }
}
