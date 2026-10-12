package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through Spring MVC's real JSON binding and the real SecurityConfig (/auth/** is public):
 * a register request containing "role":"ADMIN" is accepted and still creates a CANDIDATE.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthService.class})
class AuthControllerRegistrationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository appUserRepository;
    @MockBean
    private JwtService jwtService;

    @Test
    void registerWithAdminRoleInBodyStillCreatesCandidate() throws Exception {
        when(appUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(jwtService.generateToken(anyString())).thenReturn("jwt");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"secret\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userRole").value("CANDIDATE"))
                .andExpect(jsonPath("$.token").value("jwt"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.CANDIDATE);
        // real BCrypt PasswordEncoder from SecurityConfig
        assertThat(saved.getValue().getPassword()).startsWith("$2");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\":\"not-an-email\",\"password\":\"secret\"}",
            "{\"email\":\"new@example.com\",\"password\":\"1\"}",
            "{\"email\":\"new@example.com\"}",
            "{}"
    })
    void invalidRegistrationIsRejectedWith400(String body) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void loginWithEmptyBodyIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
