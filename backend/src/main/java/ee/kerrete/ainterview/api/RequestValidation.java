package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.auth.CurrentUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The one shared helper for request bodies whose email is filled from the auth principal after binding
 * (so {@code @Valid} on the parameter would reject requests without a body email): set the email first,
 * then validate the rest of the body. Violations become 400 with a sorted "field message" list.
 * FP-25: replaces the copies that lived in TrainingProgressController and LearningPlanController.
 */
final class RequestValidation {

    private RequestValidation() {
    }

    /** Sets the authenticated user's email on {@code request} and validates it. */
    static <T> T withPrincipalEmail(Validator validator, T request, BiConsumer<T, String> emailSetter) {
        emailSetter.accept(request, CurrentUser.email());
        validate(validator, request);
        return request;
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
