package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.AdaptiveQuestionRequest;
import ee.kerrete.ainterview.dto.AdaptiveQuestionResponse;
import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import ee.kerrete.ainterview.dto.EvaluateAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI kliendi wrapper: vastuse hindamine + adaptiivne järgmine küsimus.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AiEvaluationClient {

    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:${OPENAI_API_KEY:dummy-openai-key}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private String chatCompletionsUrl() {
        return baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";
    }

    // =========================================================
    //  1. VASTUSE HINDAMINE (STAR, 0–100, strengths/weaknesses)
    // =========================================================
    public EvaluateAnswerResponse evaluateStarAnswer(EvaluateAnswerRequest request) {
        try {
            String url = chatCompletionsUrl();

            String systemPrompt = """
                You are an expert technical interview coach.
                Evaluate the candidate's answer using the STAR method (Situation, Task, Action, Result).
                Output MUST be ONLY a single JSON object, no markdown, no backticks, no commentary.
                Schema:
                {
                  "score": number,             // 0–100
                  "strengths": string,         // what was done well
                  "weaknesses": string,        // what is missing
                  "suggestions": string        // how to improve
                }
                """;

            String userPrompt = String.format(
                    "Question: %s%n%nCandidate answer:%n%s",
                    nullToEmpty(request.getQuestion()),
                    nullToEmpty(request.getAnswer())
            );

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = responseEntity.getBody();
            log.debug("OpenAI raw evaluation response: {}", responseBody);

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Empty response from OpenAI");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response");
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("Empty content in OpenAI response");
            }

            String jsonString = extractJsonObject(content);
            JsonNode json = objectMapper.readTree(jsonString);

            int score = json.path("score").asInt(0);
            String strengths = json.path("strengths").asText("");
            String weaknesses = json.path("weaknesses").asText("");
            String suggestions = json.path("suggestions").asText("");

            EvaluateAnswerResponse result = new EvaluateAnswerResponse();
            result.setScore(score);
            result.setStrengths(strengths);
            result.setWeaknesses(weaknesses);
            result.setSuggestions(suggestions);
            result.setFallback(false); // GPT töötas

            return result;
        } catch (Exception e) {
            log.error("OpenAI evaluation error", e);
            throw new RuntimeException("OpenAI evaluation failed: " + e.getMessage(), e);
        }
    }

    // =========================================================
    //  2. ADAPTIIVNE JÄRGMINE KÜSIMUS
    // =========================================================
    public AdaptiveQuestionResponse generateAdaptiveQuestion(AdaptiveQuestionRequest request) {
        try {
            String url = chatCompletionsUrl();

            String systemPrompt = """
                You are an expert interview trainer.
                The user is practicing answers using the STAR method: Situation, Task, Action, Result.
                You get the user's *previous answer*.
                Your job:
                1) Analyse which STAR component is weakest or missing.
                2) Generate ONE follow-up question that helps the user improve exactly that weak component.
                
                Rules:
                - Focus on a single concrete aspect (Situation OR Task OR Action OR Result).
                - Use friendly, concise language.
                - Output MUST be ONLY a JSON object, no markdown, no code fences, no explanation.
                
                JSON schema:
                {
                  "questionText": string,   // the follow-up coaching question
                  "fallback": false         // always false when you produce this JSON
                }
                """;

            String userPrompt = "Previous answer from the candidate:\n\n"
                    + nullToEmpty(request.getPreviousAnswer());

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

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = responseEntity.getBody();
            log.debug("OpenAI raw adaptive-question response: {}", responseBody);

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Empty response from OpenAI (adaptive question)");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("No choices in OpenAI response (adaptive question)");
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("Empty content in OpenAI response (adaptive question)");
            }

            String jsonString = extractJsonObject(content);
            JsonNode json = objectMapper.readTree(jsonString);

            String questionText = json.path("questionText")
                    .asText("Selgita veidi täpsemalt oma rolli selles olukorras (üks STAR komponent).");

            return new AdaptiveQuestionResponse(questionText, false);
        } catch (Exception e) {
            log.error("OpenAI adaptive question error", e);
            // Fallback: lihtne Result/Action küsimus
            return new AdaptiveQuestionResponse(
                    "Mis oli sinu tegevuse konkreetne tulemus? Proovi tuua välja numbreid või mõõdetav mõju (Result).",
                    true
            );
        }
    }

    // =========================================================
    //  ABIMEETODID
    // =========================================================
    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    /**
     * Leiab antud stringist esimese JSON-objekti "{" ... "}" ja tagastab selle.
     * Lahendab olukorra, kus GPT tagastab ```json ... ``` ümber.
     */
    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("Could not locate JSON object in content: " + content);
        }
        return content.substring(start, end + 1).trim();
    }
}
