package ee.kerrete.ainterview.interview.api;

import com.fasterxml.jackson.databind.JsonNode;
import ee.kerrete.ainterview.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewSessionFlowControllerTest extends AbstractIntegrationTest {

    @BeforeEach
    void stubAi() {
        when(openAiClient.complete(anyString()))
            .thenReturn("""
                [
                  {"question":"Q1?","modelAnswerHint":"H1"},
                  {"question":"Q2?","modelAnswerHint":"H2"},
                  {"question":"Q3?","modelAnswerHint":"H3"},
                  {"question":"Q4?","modelAnswerHint":"H4"},
                  {"question":"Q5?","modelAnswerHint":"H5"}
                ]
                """);
    }

    @Test
    void startAndCompleteInterviewFlow() throws Exception {
        createUser("interview@example.com", "Password1!", true, ee.kerrete.ainterview.model.UserRole.USER);
        String token = loginAndGetToken("interview@example.com", "Password1!");

        String startBody = """
            {
              "companyName":"Acme",
              "roleTitle":"Backend Engineer",
              "seniority":"Senior",
              "interviewerStyle":"TECH"
            }
            """;

        var startResult = mockMvc.perform(post("/api/interview/session/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(startBody))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode startJson = objectMapper.readTree(startResult.getResponse().getContentAsString());
        UUID sessionId = UUID.fromString(startJson.path("sessionId").asText());
        int questionNumber = startJson.path("questionNumber").asInt(1);
        int totalQuestions = startJson.path("totalQuestions").asInt(5);
        for (int i = 0; i < totalQuestions - 1; i++) {
            String answerBody = """
                {"sessionId":"%s","questionNumber":%d,"answer":"Answer %d"}
                """.formatted(sessionId, questionNumber, questionNumber);

            var ans = mockMvc.perform(post("/api/interview/session/answer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .content(answerBody))
                .andExpect(status().isOk())
                .andReturn();

            JsonNode node = objectMapper.readTree(ans.getResponse().getContentAsString());
            boolean finished = node.path("isFinished").asBoolean(false);
            if (!finished) {
                questionNumber = node.get("questionNumber").asInt();
                assertThat(node.get("question").asText()).isNotBlank();
            }
        }

        // final answer to finish
        String finalAnswer = """
            {"sessionId":"%s","questionNumber":%d,"answer":"Final answer with metrics and team"}
            """.formatted(sessionId, questionNumber);

        mockMvc.perform(post("/api/interview/session/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(finalAnswer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isFinished").value(true))
            .andExpect(jsonPath("$.fitScore").exists())
            .andExpect(jsonPath("$.strengths").isArray())
            .andExpect(jsonPath("$.dimensionScores").isArray());
    }
}

