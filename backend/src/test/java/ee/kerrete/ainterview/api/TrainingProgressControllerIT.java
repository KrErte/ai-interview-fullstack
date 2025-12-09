package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TrainingProgressControllerIT extends AbstractIntegrationTest {

    @Test
    void getProgress_withAuth_returnsOk() throws Exception {
        createUser("progress@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("progress@example.com", "Password1!");

        mockMvc.perform(get("/api/training/progress")
                .param("email", "progress@example.com")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void getProgress_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/training/progress")
                .param("email", "unauth@example.com"))
            .andExpect(status().isUnauthorized());
    }
}

