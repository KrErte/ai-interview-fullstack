package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.ai.AiUnavailableException;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.config.OpenAiProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientRateLimitTest {

    @Test
    void completeThrowsBeforeAnyHttpCallWhenTheLimiterRefuses() {
        OpenAiProperties props = new OpenAiProperties();
        props.setApiKey("sk-test");
        props.setBaseUrl("https://openai.invalid/v1");
        OpenAiClient client = new OpenAiClient(props, LlmRateLimiter.blocking());

        assertThatThrownBy(() -> client.complete("hello"))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("rate limit");
    }
}
