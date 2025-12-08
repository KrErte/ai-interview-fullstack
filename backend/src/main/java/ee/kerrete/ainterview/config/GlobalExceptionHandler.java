package ee.kerrete.ainterview.config;

import lombok.extern.slf4j.Slf4j;
import ee.kerrete.ainterview.exception.InvalidCredentialsException;
import ee.kerrete.ainterview.exception.UserDisabledException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Handling InvalidCredentialsException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("message", "Invalid credentials"));
    }

    @ExceptionHandler(UserDisabledException.class)
    public ResponseEntity<Map<String, String>> handleUserDisabled(UserDisabledException ex) {
        log.warn("Handling UserDisabledException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of("message", "User is disabled"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {

        HttpStatusCode status = ex.getStatusCode();
        String message = ex.getReason();

        log.warn("Handling ResponseStatusException: status={}, reason={}, message={}", status, ex.getReason(), ex.getMessage());

        // Provide correct fallback messages expected by tests
        if (message == null || message.isBlank()) {
            if (status.value() == 401) {
                message = "Invalid credentials";
            } else if (status.value() == 403) {
                message = "User is disabled";
            } else {
                message = "Unexpected error";
            }
        }

        return ResponseEntity
                .status(status)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Handling BadCredentialsException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Handling unexpected exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Unexpected error"));
    }
}
