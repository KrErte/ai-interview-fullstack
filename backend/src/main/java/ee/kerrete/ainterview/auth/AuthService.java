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
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user. Self-registration always gets the CANDIDATE role (the existing default);
     * a role can never be chosen by the client, so nobody can self-register as ADMIN.
     */
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        // Kontroll: kas sama emailiga kasutaja juba eksisteerib
        appUserRepository.findByEmail(email)
                .ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User with this email already exists");
                });

        LocalDateTime now = LocalDateTime.now();

        UserRole role = UserRole.CANDIDATE;

        AppUser user = AppUser.builder()
                .email(email)
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

        // Uued kontod on salvestatud väiketähtedega; vanad segatähtedega kontod leitakse täpse kirjapildi järgi.
        String email = normalizeEmail(request.getEmail());
        AppUser user = appUserRepository.findByEmail(email)
                .or(() -> appUserRepository.findByEmail(request.getEmail().trim()))
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

    /**
     * E-post on tõstutundetu: "Demo@Example.com " ja "demo@example.com" on sama konto.
     */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
