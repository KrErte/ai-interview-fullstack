package ee.kerrete.ainterview.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for non-auth related exceptions.
 * Auth exceptions are handled by AuthExceptionHandler.
 */
@RestControllerAdvice
@Order(0)  // Lower precedence than AuthExceptionHandler
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = ex.getReason();

        log.warn("ResponseStatusException: status={}, reason={}", status, ex.getReason());

        // Provide fallback messages for common status codes
        if (message == null || message.isBlank()) {
            message = switch (status.value()) {
                case 401 -> "Invalid credentials";
                case 403 -> "Access denied";
                case 404 -> "Not found";
                default -> "Unexpected error";
            };
        }

        return ResponseEntity
            .status(status)
            .body(Map.of("message", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "Invalid value")
            .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "Unexpected error"));
    }
}
