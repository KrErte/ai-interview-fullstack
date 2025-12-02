package ee.krerte.aiinterview.auth;

import ee.krerte.aiinterview.auth.dto.LoginRequest;
import ee.krerte.aiinterview.auth.dto.RegisterRequest;
import ee.krerte.aiinterview.model.AppUser;
import ee.krerte.aiinterview.model.UserRole;
import ee.krerte.aiinterview.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registreeri uus kasutaja – vaikimisi roll CANDIDATE.
     */
    public ee.krerte.aiinterview.auth.AuthResponse register(RegisterRequest request) {

        // Kontroll: kas sama emailiga kasutaja juba eksisteerib
        appUserRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("User with this email already exists");
                });

        LocalDateTime now = LocalDateTime.now();

        AppUser user = AppUser.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CANDIDATE)
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        appUserRepository.save(user);

        // JwtService eeldab tõenäoliselt Stringi (email / username)
        String token = jwtService.generateToken(user.getEmail());

        return ee.krerte.aiinterview.auth.AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * Logi sisse olemasoleva kasutajana.
     */
    public ee.krerte.aiinterview.auth.AuthResponse login(LoginRequest request) {

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // OLULINE: ära kodeeri uuesti, kasuta matches()
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("User is disabled");
        }

        String token = jwtService.generateToken(user.getEmail());

        return ee.krerte.aiinterview.auth.AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
