package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.dto.UserProfileDto;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserProfileControllerIT extends AbstractIntegrationTest {

    @Test
    void saveAndFetchProfile_authenticatedUser_succeeds() throws Exception {
        createUser("profile@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("profile@example.com", "Password1!");

        UserProfileDto dto = new UserProfileDto();
        dto.setEmail("profile@example.com");
        dto.setFullName("New Name");
        dto.setSkills("Java, Spring");

        mockMvc.perform(put("/api/user/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("New Name"));

        mockMvc.perform(get("/api/user/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("New Name"))
            .andExpect(jsonPath("$.skills").value("Java, Spring"));
    }

    @Test
    void saveProfile_withoutAuth_returnsUnauthorized() throws Exception {
        UserProfileDto dto = new UserProfileDto();
        dto.setFullName("No Auth");

        mockMvc.perform(put("/api/user/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isUnauthorized());
    }
}

