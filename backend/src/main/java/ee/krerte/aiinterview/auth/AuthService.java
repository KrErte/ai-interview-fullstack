package ee.krerte.aiinterview.auth;

import ee.krerte.aiinterview.auth.dto.LoginRequest;
import ee.krerte.aiinterview.auth.dto.RegisterRequest;
import ee.krerte.aiinterview.model.AppUser;
import ee.krerte.aiinterview.model.UserRole;
import ee.krerte.aiinterview.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Login – lubab nii:
     *  - vanad seeditud kasutajad (plain text parool DB-s)
     *  - uued kasutajad (BCrypt hash)
     */
    public ee.krerte.aiinterview.auth.AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String storedPassword = user.getPassword();
        String rawPassword = request.getPassword();

        boolean matches;
        // Kui parool algab $2..., eeldame, et see on BCrypt hash
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            matches = passwordEncoder.matches(rawPassword, storedPassword);
        } else {
            // Seeditud kasutajate puhul – plain text võrdlus
            matches = storedPassword != null && storedPassword.equals(rawPassword);
        }

        if (!matches) {
            throw new RuntimeException("Wrong password");
        }

        String token = jwtService.generateToken(user.getEmail());
        return new ee.krerte.aiinterview.auth.AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole()          // tagastame UserRole, mitte .name()
        );
    }

    /**
     * Register – loob uue kasutaja, salvestab parooli BCryptiga
     * ja tagastab kohe sama struktuuri mis login (AuthResponse).
     */
    public ee.krerte.aiinterview.auth.AuthResponse register(RegisterRequest request) {

        // lihtne kontroll – sama emailiga kasutajat ei tohiks olla
        appUserRepository.findByEmail(request.getEmail())
                .ifPresent(u -> {
                    throw new RuntimeException("User already exists");
                });

        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(UserRole.USER);
        // uutele kasutajatele salvestame juba BCryptiga
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        appUserRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new ee.krerte.aiinterview.auth.AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
