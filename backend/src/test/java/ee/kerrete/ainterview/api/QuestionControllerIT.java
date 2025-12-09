package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.dto.CvQuestionsRequest;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class QuestionControllerIT extends AbstractIntegrationTest {

    @Test
    void generateQuestions_withValidCv_returnsList() throws Exception {
        createUser("q@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("q@example.com", "Password1!");

        CvQuestionsRequest req = new CvQuestionsRequest();
        req.setCvText("I worked on Java and Spring projects.");
        req.setSoftCount(1);
        req.setTechCount(1);

        mockMvc.perform(post("/api/questions/from-cv")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].question").isNotEmpty());
    }

    @Test
    void generateQuestions_withBlankCv_returnsBadRequest() throws Exception {
        createUser("q2@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("q2@example.com", "Password1!");

        CvQuestionsRequest req = new CvQuestionsRequest();
        req.setCvText("   ");
        req.setSoftCount(1);
        req.setTechCount(1);

        mockMvc.perform(post("/api/questions/from-cv")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}

