package ee.kerrete.ainterview.auth.service;

import ee.kerrete.ainterview.auth.dto.request.LoginRequest;
import ee.kerrete.ainterview.auth.dto.request.RegisterRequest;
import ee.kerrete.ainterview.auth.dto.response.AuthResponse;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service handling authentication operations: login, register, and token refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user.
     *
     * @param request Registration details
     * @return Authentication response with tokens
     * @throws ResponseStatusException if email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for existing user
        appUserRepository.findByEmailIgnoreCase(request.email())
            .ifPresent(u -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            });

        UserRole role = request.roleOrDefault();

        AppUser user = AppUser.builder()
            .email(request.email().toLowerCase().trim())
            .fullName(request.fullName().trim())
            .password(passwordEncoder.encode(request.password()))
            .role(role)
            .enabled(true)
            .build();

        user = appUserRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param request Login credentials
     * @return Authentication response with tokens
     * @throws ResponseStatusException on authentication failure
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.debug("Attempting authentication for email: {}", request.email());

            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            AppUser user = (AppUser) auth.getPrincipal();
            log.info("User logged in: {}", user.getEmail());

            return buildAuthResponse(user);

        } catch (DisabledException ex) {
            log.warn("Login failed - user disabled: {}", request.email());
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "User is disabled"
            );
        } catch (AuthenticationException ex) {
            log.warn("Login failed - invalid credentials: {}", request.email());
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            );
        }
    }

    /**
     * Refresh access token using a valid refresh token.
     *
     * @param refreshToken The refresh token
     * @return New authentication response with fresh tokens
     * @throws ResponseStatusException if refresh token is invalid
     */
    public AuthResponse refreshToken(String refreshToken) {
        // Validate it's a refresh token
        if (!jwtService.isRefreshToken(refreshToken)) {
            log.warn("Token refresh failed - not a refresh token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // Check expiration
        if (jwtService.isTokenExpired(refreshToken)) {
            log.warn("Token refresh failed - token expired");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        // Extract email and find user
        String email = jwtService.extractEmail(refreshToken);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is disabled");
        }

        log.info("Token refreshed for user: {}", email);
        return buildAuthResponse(user);
    }

    /**
     * Build authentication response with access and refresh tokens.
     */
    private AuthResponse buildAuthResponse(AppUser user) {
        String accessToken = jwtService.generateAccessToken(
            user.getEmail(),
            user.getRole(),
            user.getId()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
            .token(accessToken)           // Backward compatibility
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().name())  // String for backward compatibility
            .userId(user.getId())
            .build();
    }
}
