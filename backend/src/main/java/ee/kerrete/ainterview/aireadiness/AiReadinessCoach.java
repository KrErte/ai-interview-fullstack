package ee.kerrete.ainterview.aireadiness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.ai.LlmRateLimiter;
import ee.kerrete.ainterview.aireadiness.AssessmentModels.AssessmentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Valikuline LLM-põhine isiklik kommentaar tulemusele.
 * Kui API võti puudub või päring ebaõnnestub, tagastab tühja tulemuse – põhiskoor ja roadmap
 * ei sõltu LLM-ist.
 */
@Component
@Slf4j
public class AiReadinessCoach {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final LlmRateLimiter rateLimiter;

    @Value("${openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    /** Tests that do not exercise the rate limit. */
    public AiReadinessCoach(ObjectMapper objectMapper) {
        this(objectMapper, LlmRateLimiter.unlimited());
    }

    @Autowired
    public AiReadinessCoach(ObjectMapper objectMapper, LlmRateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(25_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("dummy");
    }

    public Optional<String> personalNote(AssessmentResult result, Map<String, String> answerLabels) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (!rateLimiter.tryAcquire(LlmRateLimiter.clientKey())) {
            log.info("AI comment skipped: rate limit");
            return Optional.empty();
        }
        try {
            String systemPrompt = """
                    You are a practical career coach who helps people adapt to AI.
                    Write 4-6 sentences in plain English: concrete and encouraging, without fearmongering or hype.
                    Name exactly one concrete action the person should take this week.
                    Do not repeat any scores or numbers the user already sees. Do not use markdown.
                    """;
            String userPrompt = """
                    Profession: %s
                    Replaceability score: %d/100 (%s)
                    AI readiness: %d/100 (%s)
                    Most automatable tasks: %s
                    Human edge: %s
                    Questionnaire answers: %s
                    """.formatted(
                    result.professionName(),
                    result.replaceabilityScore(), result.band(),
                    result.readinessScore(), result.readinessLevel(),
                    result.tasks().stream().limit(3).map(AssessmentModels.TaskExposure::name).toList(),
                    result.humanEdge(),
                    answerLabels
            );

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.5
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            JsonNode content = objectMapper.readTree(response == null ? "{}" : response)
                    .path("choices").path(0).path("message").path("content");
            String text = content.asText("").trim();
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            log.warn("AI readiness personal note failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
