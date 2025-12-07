package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.auth.dto.RegisterRequest;
import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registreeri uus kasutaja – vaikimisi roll CANDIDATE.
     */
    public AuthResponse register(RegisterRequest request) {

        // Kontroll: kas sama emailiga kasutaja juba eksisteerib
        appUserRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
                });

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

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // OLULINE: ära kodeeri uuesti, kasuta matches()
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }

        String token = jwtService.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userRole(user.getRole())
                .build();
    }
}
