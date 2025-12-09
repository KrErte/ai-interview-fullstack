package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EvaluationControllerIT extends AbstractIntegrationTest {

    @BeforeEach
    void setupEvaluationMock() {
        EvaluateAnswerResponse resp = new EvaluateAnswerResponse();
        resp.setScore(75);
        resp.setStrengths("Good answer");
        resp.setWeaknesses("Add metrics");
        resp.setSuggestions("Be concise");
        resp.setFallback(false);
        Mockito.reset(aiEvaluationClient);
        when(aiEvaluationClient.evaluateStarAnswer(any())).thenReturn(resp);
    }

    @Test
    void evaluate_withValidAnswer_returnsScore() throws Exception {
        createUser("eval@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("eval@example.com", "Password1!");

        EvaluateAnswerRequest req = new EvaluateAnswerRequest(
            "eval@example.com",
            "q1",
            "Tell me about REST.",
            "Here is my STAR answer."
        );

        mockMvc.perform(post("/api/questions/evaluate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").value(75))
            .andExpect(jsonPath("$.strengths").isNotEmpty());
    }

    @Test
    void evaluate_withEmptyAnswer_returnsBadRequest() throws Exception {
        createUser("eval2@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("eval2@example.com", "Password1!");

        EvaluateAnswerRequest req = new EvaluateAnswerRequest(
            "eval2@example.com",
            "q1",
            "Tell me about REST.",
            " "
        );

        mockMvc.perform(post("/api/questions/evaluate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}

