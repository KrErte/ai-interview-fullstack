package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.auth.dto.request.LoginRequest;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        createUser("user@example.com", "Password1!", true, UserRole.USER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "Password1!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        createUser("user@example.com", "Password1!", true, UserRole.USER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "BadPass"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withDisabledUser_returnsForbidden() throws Exception {
        createUser("disabled@example.com", "Password1!", false, UserRole.USER);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("disabled@example.com", "Password1!"))))
            .andExpect(status().isForbidden())
            .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("disabled"));
    }
}

