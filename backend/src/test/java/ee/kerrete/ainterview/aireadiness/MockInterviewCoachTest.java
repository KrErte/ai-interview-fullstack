package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.JobPages.InterviewQuestion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MockInterviewCoachTest {

    static final InterviewQuestion QUESTION = new InterviewQuestion("How do you use AI in your work today?",
            "That you use AI on purpose.", "Name a tool and a task.");
    static final String ANSWER = "I use Excel Copilot to draft reconciliations and check every total against the bank.";

    final ObjectMapper json = new ObjectMapper();
    final RestTemplate rest = new RestTemplate();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();

    MockInterviewCoach coach(String key, LlmRateLimiter limiter) {
        return new MockInterviewCoach(json, key, "https://llm.example/v1", "gpt-test", limiter, rest);
    }

    static String completion(String content) throws Exception {
        return new ObjectMapper().writeValueAsString(
                java.util.Map.of("choices", java.util.List.of(java.util.Map.of("message", java.util.Map.of("content", content)))));
    }

    @Test
    void scoresTheAnswerWithTheRubricAndKeepsOnlyTheUsersFacts() throws Exception {
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(content().string(containsString("\"model\":\"gpt-test\"")))
                .andExpect(content().string(containsString("\"response_format\":{\"type\":\"json_object\"}")))
                .andExpect(content().string(containsString("Profession: Accountant")))
                .andExpect(content().string(containsString("check every total against the bank")))
                .andExpect(content().string(containsString("Never invent tools, numbers")))
                .andRespond(withSuccess(completion("""
                        {"level":"Capable","strengths":["Names a real tool","Checks the output","x","y"],
                         "missing":["No time saved"],"improvedAnswer":"I use Excel Copilot ... saving [minutes saved]."}
                        """), MediaType.APPLICATION_JSON));

        MockInterviewCoach.Evaluation e = coach("sk-test", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER);

        assertThat(e.level()).isEqualTo("Capable");
        assertThat(e.score()).isEqualTo(2);
        assertThat(e.strengths()).containsExactly("Names a real tool", "Checks the output", "x");
        assertThat(e.missing()).containsExactly("No time saved");
        assertThat(e.improvedAnswer()).contains("[minutes saved]");
        server.verify();
    }

    @Test
    void withoutAKeyNothingIsSent() {
        assertThatThrownBy(() -> coach("", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> coach("dummy-key", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
        server.verify();
    }

    @Test
    void overTheLimitIsRefusedBeforeAnyCall() {
        assertThatThrownBy(() -> coach("sk-test", LlmRateLimiter.blocking()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(MockInterviewCoach.LimitReachedException.class);
        server.verify();
    }

    @Test
    void anUnknownLevelOrAFailedCallIsUnavailable() throws Exception {
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess(completion("{\"level\":\"Genius\",\"improvedAnswer\":\"x\"}"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> coach("sk-test", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);

        server.reset();
        server.expect(requestTo("https://llm.example/v1/chat/completions")).andRespond(withServerError());
        assertThatThrownBy(() -> coach("sk-test", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
    }
}
