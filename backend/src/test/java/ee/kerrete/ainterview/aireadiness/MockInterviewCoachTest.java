package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.JobPages.InterviewQuestion;
import ee.kerrete.ainterview.aireadiness.MockInterviewCoach.Provider;
import ee.kerrete.ainterview.aireadiness.MockInterviewCoach.Settings;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

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

    MockInterviewCoach coach(String anthropicKey, String openAiKey, LlmRateLimiter limiter) {
        return new MockInterviewCoach(json, new Settings(anthropicKey, "https://claude.example/v1", "claude-test",
                openAiKey, "https://llm.example/v1", "gpt-test"), limiter, rest);
    }

    static String completion(String content) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
    }

    @Test
    void claudeWinsWhenBothKeysAreSetAndNoKeyMeansOff() {
        assertThat(coach("sk-ant", "sk-oai", LlmRateLimiter.unlimited()).provider()).isEqualTo(Provider.ANTHROPIC);
        assertThat(coach("", "sk-oai", LlmRateLimiter.unlimited()).provider()).isEqualTo(Provider.OPENAI);
        assertThat(coach("dummy", "", LlmRateLimiter.unlimited()).provider()).isNull();
        assertThat(coach(null, null, LlmRateLimiter.unlimited()).isEnabled()).isFalse();
    }

    @Test
    void claudeIsAskedWithAForcedToolAndItsToolInputIsTheEvaluation() throws Exception {
        server.expect(requestTo("https://claude.example/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "sk-ant"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().string(containsString("\"model\":\"claude-test\"")))
                .andExpect(content().string(containsString("\"tool_choice\":{\"type\":\"tool\",\"name\":\"record_evaluation\"}")))
                .andExpect(content().string(containsString("\"enum\":[\"Unacceptable\",\"Capable\",\"Adoptive\",\"Transformative\"]")))
                .andExpect(content().string(containsString("Never invent tools, numbers")))
                .andExpect(content().string(containsString("check every total against the bank")))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"Here you go"},
                          {"type":"tool_use","name":"record_evaluation","input":{"level":"Adoptive",
                           "strengths":["Real tool"],"missing":["Time saved"],"improvedAnswer":"I use Excel Copilot ... [minutes saved]."}}]}
                        """, MediaType.APPLICATION_JSON));

        MockInterviewCoach.Evaluation e = coach("sk-ant", "sk-oai", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER);

        assertThat(e.level()).isEqualTo("Adoptive");
        assertThat(e.score()).isEqualTo(3);
        assertThat(e.missing()).containsExactly("Time saved");
        assertThat(e.improvedAnswer()).contains("[minutes saved]");
        server.verify();
    }

    @Test
    void claudeWithoutTheToolCallIsUnavailable() {
        server.expect(requestTo("https://claude.example/v1/messages"))
                .andRespond(withSuccess("{\"content\":[{\"type\":\"text\",\"text\":\"Adoptive\"}]}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> coach("sk-ant", "", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void openAiUsesJsonModeAndKeepsOnlyTheUsersFacts() throws Exception {
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-oai"))
                .andExpect(content().string(containsString("\"model\":\"gpt-test\"")))
                .andExpect(content().string(containsString("\"response_format\":{\"type\":\"json_object\"}")))
                .andExpect(content().string(containsString("Profession: Accountant")))
                .andRespond(withSuccess(completion("""
                        {"level":"Capable","strengths":["Names a real tool","Checks the output","x","y"],
                         "missing":["No time saved"],"improvedAnswer":"I use Excel Copilot ... saving [minutes saved]."}
                        """), MediaType.APPLICATION_JSON));

        MockInterviewCoach.Evaluation e = coach("", "sk-oai", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER);

        assertThat(e.level()).isEqualTo("Capable");
        assertThat(e.score()).isEqualTo(2);
        assertThat(e.strengths()).containsExactly("Names a real tool", "Checks the output", "x");
        server.verify();
    }

    @Test
    void withoutAKeyNothingIsSentAndOverTheLimitIsRefused() {
        assertThatThrownBy(() -> coach("", "", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
        assertThatThrownBy(() -> coach("sk-ant", "", LlmRateLimiter.blocking()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(MockInterviewCoach.LimitReachedException.class);
        server.verify();
    }

    @Test
    void anUnknownLevelOrAFailedCallIsUnavailable() throws Exception {
        server.expect(requestTo("https://llm.example/v1/chat/completions"))
                .andRespond(withSuccess(completion("{\"level\":\"Genius\",\"improvedAnswer\":\"x\"}"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> coach("", "sk-oai", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);

        server.reset();
        server.expect(requestTo("https://claude.example/v1/messages")).andRespond(withServerError());
        assertThatThrownBy(() -> coach("sk-ant", "", LlmRateLimiter.unlimited()).evaluate("Accountant", QUESTION, ANSWER))
                .isInstanceOf(AiUnavailableException.class);
    }
}
