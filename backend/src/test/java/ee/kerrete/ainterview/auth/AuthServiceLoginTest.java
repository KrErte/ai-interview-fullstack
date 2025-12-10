package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.request.LoginRequest;
import ee.kerrete.ainterview.auth.service.AuthService;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private AppUser enabledUser;

    @BeforeEach
    void setUp() {
        enabledUser = AppUser.builder()
            .id(1L)
            .email("user@test.com")
            .fullName("Test User")
            .password("encoded-password")
            .role(UserRole.CANDIDATE)
            .enabled(true)
            .build();
    }

    @Test
    void testLogin_DisabledUser_ThrowsForbidden() {
        LoginRequest request = new LoginRequest("disabled@test.com", "password");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new DisabledException("User is disabled"));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(request)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("User is disabled", ex.getReason());
    }

    @Test
    void testLogin_InvalidPassword_ThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong-password");

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid email or password", ex.getReason());
    }

    @Test
    void testLogin_ValidCredentials_ReturnsTokens() {
        LoginRequest request = new LoginRequest("user@test.com", "correct-password");

        when(authenticationManager.authenticate(any()))
            .thenAnswer(invocation -> {
                return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    enabledUser,
                    request.password(),
                    enabledUser.getAuthorities()
                );
            });

        when(jwtService.generateAccessToken(enabledUser.getEmail(), enabledUser.getRole(), enabledUser.getId()))
            .thenReturn("access-token");
        when(jwtService.generateRefreshToken(enabledUser.getEmail()))
            .thenReturn("refresh-token");

        var response = authService.login(request);

        // AuthResponse on sul builderiga record – kasutame komponentmeetodeid, mitte getX()
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("user@test.com", response.email());
    }
}
