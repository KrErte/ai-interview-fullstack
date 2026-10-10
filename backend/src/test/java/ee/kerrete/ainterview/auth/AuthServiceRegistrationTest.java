package ee.kerrete.ainterview.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.RegisterRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FP-8: self-registration always creates a CANDIDATE; a client-supplied role is ignored.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;

    // Plain ObjectMapper (FAIL_ON_UNKNOWN_PROPERTIES = true by default): RegisterRequest must
    // still accept a "role" property thanks to @JsonIgnoreProperties(ignoreUnknown = true).
    private final ObjectMapper strictObjectMapper = new ObjectMapper();

    private AppUser registerAndCaptureSavedUser(String json) throws Exception {
        when(appUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(jwtService.generateToken("new@example.com")).thenReturn("jwt");

        AuthResponse res = authService.register(strictObjectMapper.readValue(json, RegisterRequest.class));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());
        assertThat(res.getUserRole()).isEqualTo(saved.getValue().getRole());
        assertThat(res.getToken()).isEqualTo("jwt");
        return saved.getValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SUPERUSER", "USER", "CANDIDATE"})
    void clientSuppliedRoleIsIgnored(String role) throws Exception {
        AppUser saved = registerAndCaptureSavedUser("{\"email\":\"new@example.com\",\"password\":\"secret\","
                + "\"fullName\":\"New\",\"role\":\"" + role + "\"}");

        assertThat(saved.getRole()).isEqualTo(UserRole.CANDIDATE);
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registrationWithoutRoleGetsCandidateRole() throws Exception {
        AppUser saved = registerAndCaptureSavedUser("{\"email\":\"new@example.com\",\"password\":\"secret\"}");
        assertThat(saved.getRole()).isEqualTo(UserRole.CANDIDATE);
    }

    @Test
    void cannotSelfRegisterAsAdmin() throws Exception {
        AppUser saved = registerAndCaptureSavedUser(
                "{\"email\":\"new@example.com\",\"password\":\"secret\",\"role\":\"ADMIN\"}");
        assertThat(saved.getRole()).isNotEqualTo(UserRole.ADMIN).isEqualTo(UserRole.CANDIDATE);
    }

    @Test
    void existingEmailIsRejected() {
        when(appUserRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(AppUser.builder().email("taken@example.com").role(UserRole.ADMIN).build()));
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@example.com");
        req.setPassword("x");

        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(ResponseStatusException.class);
        verify(appUserRepository, never()).save(any());
        verify(jwtService, never()).generateToken(anyString());
    }
}
