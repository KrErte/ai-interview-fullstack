package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.debug("Login request received for email: {}", req.getEmail());
        try {
            AuthResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            HttpStatusCode status = ex.getStatusCode();
            String message = ex.getReason();
            if (message == null || message.isBlank()) {
                message = ex.getMessage();
            }
            return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message));
        }
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        log.debug("Register request received for email: {}", req.getEmail());
        return authService.register(req);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Auth request validation failed: {}", errors);
        return errors;
    }
}
