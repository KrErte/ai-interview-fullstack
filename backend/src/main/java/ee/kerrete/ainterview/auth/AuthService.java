package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.auth.dto.RegisterRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registreeri uus kasutaja – vaikimisi roll CANDIDATE.
     */
    public AuthResponse register(RegisterRequest request) {

        // Kontroll: kas sama emailiga kasutaja juba eksisteerib
        appUserRepository.findByEmail(request.getEmail())
            .ifPresent(u -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
            });

        if (request.getConfirmPassword() != null
            && !request.getConfirmPassword().isBlank()
            && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        LocalDateTime now = LocalDateTime.now();

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CANDIDATE;

        AppUser user = AppUser.builder()
            .email(request.getEmail())
            .fullName(request.getFullName())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .enabled(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

        appUserRepository.save(Objects.requireNonNull(user));

        // JwtService eeldab tõenäoliselt Stringi (email / username)
        String token = jwtService.generateToken(user.getEmail());

        log.info("User registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
            .token(token)
            .email(user.getEmail())
            .fullName(user.getFullName())
            .userRole(user.getRole())
            .build();
    }

    /**
     * Logi sisse olemasoleva kasutajana.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.debug("Attempting authentication for email: {}", request.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            AppUser user = (AppUser) authentication.getPrincipal();
            String token = jwtService.generateToken(user.getEmail());

            log.info("User logged in successfully: {}", user.getEmail());

            return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userRole(user.getRole())
                .build();
        } catch (DisabledException ex) {
            log.warn("Login failed for {}: user is disabled", request.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        } catch (AuthenticationException ex) {
            log.warn("Login failed for {}: invalid credentials", request.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        } catch (Exception ex) {
            log.error("Unexpected error during login for {}", request.getEmail(), ex);
            throw new AuthenticationServiceException("Authentication failed", ex);
        }
    }
}
