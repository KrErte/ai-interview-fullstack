package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisRequest;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAdaptiveService {

    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:${OPENAI_API_KEY:dummy-openai-key}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public AdaptiveAnalysisResponse analyzeAnswer(AdaptiveAnalysisRequest request) {
        try {
            String prompt = buildPrompt(request);

            // --- Ehita request body OpenAI jaoks (Map, mitte JsonNode) ---
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content",
                    "Sa oled kogenud engineering manager ja coach. " +
                            "Hinda vastust ja tagasta tulemus JSON-is kujul: " +
                            "{\"skillScores\": {communication:number, conflict_management:number, ownership:number, leadership:number}," +
                            "\"weakestSkill\":\"skill_name\", \"coachFeedback\":\"tekst\", \"nextQuestion\":\"tekst\"}"
            ));
            messages.add(Map.of(
                    "role", "user",
                    "content", prompt
            ));
            requestBody.put("messages", messages);

            String url = baseUrl.endsWith("/chat/completions")
                    ? baseUrl
                    : baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("OpenAI HTTP error: status={} body={}",
                        response.getStatusCode(), response.getBody());
                return AdaptiveAnalysisResponse.error("OpenAI vastus ei olnud edukas.");
            }

            String responseBody = response.getBody();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            String content = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content == null || content.isBlank()) {
                return AdaptiveAnalysisResponse.error("OpenAI ei tagastanud sisu.");
            }

            // Mudel võib panna ```json wrapperi ümber
            String cleaned = content
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode json;
            try {
                json = objectMapper.readTree(cleaned);
            } catch (IOException e) {
                log.error("Ei suutnud OpenAI JSON-i parsida. content={}", cleaned, e);
                return AdaptiveAnalysisResponse.error("OpenAI vastuse parsimine ebaõnnestus.");
            }

            Map<String, Integer> scores = new HashMap<>();
            JsonNode scoreNode = json.path("skillScores");
            if (scoreNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = scoreNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    scores.put(entry.getKey(), entry.getValue().asInt());
                }
            }

            String weakestSkill = json.path("weakestSkill").asText(null);
            String coachFeedback = json.path("coachFeedback").asText("");
            String nextQuestion = json.path("nextQuestion").asText("");

            return AdaptiveAnalysisResponse.builder()
                    .feedback(coachFeedback)
                    .nextQuestion(nextQuestion)
                    .updatedSkills(scores)
                    .weakestSkill(weakestSkill)
                    .build();

        } catch (Exception e) {
            log.error("Viga AI skill coachi kutsel", e);
            return AdaptiveAnalysisResponse.error("Sisemine viga vastuse analüüsimisel.");
        }
    }

    private String buildPrompt(AdaptiveAnalysisRequest request) {
        return """
                Küsimus: %s
                Vastus: %s
                Skillsnapshot: %s

                Hinda vastust ja tagasta JSON kujul:
                {
                  "skillScores": {communication, conflict_management, ownership, leadership},
                  "weakestSkill": "...",
                  "coachFeedback": "...",
                  "nextQuestion": "..."
                }
                """.formatted(
                request.getQuestion(),
                request.getAnswer(),
                request.getCurrentSkillSnapshot()
        );
    }
}
