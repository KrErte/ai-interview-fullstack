package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.request.LoginRequest;
import ee.kerrete.ainterview.auth.dto.response.AuthResponse;
import ee.kerrete.ainterview.auth.jwt.JwtService;
import ee.kerrete.ainterview.auth.service.AuthService;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ACCESS_TOKEN = "access-token-123";
    private static final String TEST_REFRESH_TOKEN = "refresh-token-456";
    private static final String TEST_NAME = "Test User";
    private static final Long TEST_USER_ID = 1L;

    @Test
    void testLogin_ValidCredentials_ReturnsTokens() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        AppUser user = AppUser.builder()
                .id(TEST_USER_ID)
                .email(TEST_EMAIL)
                .fullName(TEST_NAME)
                .role(UserRole.CANDIDATE)
                .enabled(true)
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(user);
        when(jwtService.generateAccessToken(eq(TEST_EMAIL), eq(UserRole.CANDIDATE), eq(TEST_USER_ID)))
                .thenReturn(TEST_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_EMAIL)).thenReturn(TEST_REFRESH_TOKEN);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals(TEST_ACCESS_TOKEN, response.token());
        assertEquals(TEST_ACCESS_TOKEN, response.accessToken());
        assertEquals(TEST_REFRESH_TOKEN, response.refreshToken());
        assertEquals(TEST_EMAIL, response.email());
        assertEquals(TEST_NAME, response.fullName());
        assertEquals(UserRole.CANDIDATE.name(), response.role());
        assertEquals(TEST_USER_ID, response.userId());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(TEST_EMAIL, UserRole.CANDIDATE, TEST_USER_ID);
        verify(jwtService).generateRefreshToken(TEST_EMAIL);
    }

    @Test
    void testLogin_InvalidPassword_ThrowsUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid credentials"));
        verify(jwtService, never()).generateAccessToken(anyString(), any(), any());
    }

    @Test
    void testLogin_DisabledUser_ThrowsForbidden() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("User is disabled"));
        verify(jwtService, never()).generateAccessToken(anyString(), any(), any());
    }
}
