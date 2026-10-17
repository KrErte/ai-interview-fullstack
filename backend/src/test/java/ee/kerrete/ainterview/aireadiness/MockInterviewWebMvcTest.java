package ee.kerrete.ainterview.aireadiness;

import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.auth.JwtService;
import ee.kerrete.ainterview.config.SecurityConfig;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /api/ai-readiness/mock-interview: public, the question comes from the server, 400/404/429/503 as documented. */
@WebMvcTest(controllers = MockInterviewController.class)
@Import({SecurityConfig.class, AiReadinessCatalog.class, AiReadinessScorer.class, JobPages.class})
class MockInterviewWebMvcTest {

    static final String ANSWER = "I use Claude to draft contract summaries and I check every clause myself.";

    @Autowired
    MockMvc mvc;

    @MockBean
    MockInterviewCoach coach;
    @MockBean
    JwtService jwtService;
    @MockBean
    AppUserRepository appUserRepository;

    ResultActions send(String profession, int index, String answer) throws Exception {
        String body = "{\"professionId\":\"" + profession + "\",\"questionIndex\":" + index + ",\"answer\":\"" + answer + "\"}";
        return mvc.perform(post("/api/ai-readiness/mock-interview").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void scoresTheAnswerToTheServersOwnQuestionWithoutLogin() throws Exception {
        when(coach.evaluate(anyString(), any(), anyString())).thenReturn(new MockInterviewCoach.Evaluation(
                "Adoptive", 3, List.of("Real tool"), List.of("No numbers"), "I use Claude ..."));

        send("lawyer", 2, ANSWER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("Adoptive"))
                .andExpect(jsonPath("$.score").value(3))
                .andExpect(jsonPath("$.improvedAnswer").value("I use Claude ..."));
        verify(coach).evaluate(eq("Lawyer"),
                argThat(q -> q.question().equals("Tell us about a time AI gave you a wrong or poor result. What did you do?")),
                eq(ANSWER));
    }

    @Test
    void statusSaysWhichModelScoresOrNull() throws Exception {
        when(coach.provider()).thenReturn(MockInterviewCoach.Provider.ANTHROPIC);
        mvc.perform(get("/api/ai-readiness/mock-interview/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("anthropic"));
        when(coach.provider()).thenReturn(null);
        mvc.perform(get("/api/ai-readiness/mock-interview/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").doesNotExist());
    }

    @Test
    void badInputIs400Or404AndCallsNothing() throws Exception {
        send("lawyer", 2, "too short").andExpect(status().isBadRequest());
        send("lawyer", 6, ANSWER).andExpect(status().isBadRequest());
        send("lawyer", -1, ANSWER).andExpect(status().isBadRequest());
        send("Lawyer!", 0, ANSWER).andExpect(status().isBadRequest());
        send("nope", 0, ANSWER).andExpect(status().isNotFound());
        verify(coach, never()).evaluate(anyString(), any(), anyString());
    }

    @Test
    void noKeyIs503AndTheLimitIs429() throws Exception {
        when(coach.evaluate(anyString(), any(), anyString())).thenThrow(AiUnavailableException.noApiKey());
        send("accountant", 0, ANSWER).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("ai_unavailable"));

        doThrow(new MockInterviewCoach.LimitReachedException()).when(coach).evaluate(anyString(), any(), anyString());
        send("accountant", 0, ANSWER).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.error").value("rate_limited"));
    }
}
