package ee.kerrete.ainterview.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Programmatic bean validation for request bodies whose email is filled from the auth principal
 * after binding (so {@code @Valid} on the parameter would reject requests without a body email).
 */
final class RequestValidation {

    private RequestValidation() {
    }

    static void validate(Validator validator, Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
