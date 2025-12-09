package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JobAnalysisControllerIT extends AbstractIntegrationTest {

    @Test
    void analyze_withValidPayload_returnsRoadmap() throws Exception {
        createUser("job@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("job@example.com", "Password1!");

        JobAnalysisRequest req = new JobAnalysisRequest();
        req.setEmail("job@example.com");
        req.setCvText("Java developer with Spring experience");
        req.setJobDescription("Looking for Java backend engineer");

        mockMvc.perform(post("/api/job-analysis")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matchScore").exists())
            .andExpect(jsonPath("$.roadmap").isArray());
    }

    @Test
    void analyze_missingJobDescription_returnsBadRequest() throws Exception {
        createUser("job2@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("job2@example.com", "Password1!");

        JobAnalysisRequest req = new JobAnalysisRequest();
        req.setEmail("job2@example.com");
        req.setCvText("Some CV text");
        req.setJobDescription(" ");

        mockMvc.perform(post("/api/job-analysis")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}

