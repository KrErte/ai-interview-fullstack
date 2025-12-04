package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.EvaluateAnswerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIExampleAnswerService {

    @Value("${openai.api-key:${OPENAI_API_KEY:dummy-openai-key}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String generateExampleStarAnswer(String question) throws Exception {
        String prompt = """
                You are a senior software engineer preparing a STAR interview answer.
                Write a professional STAR answer to the following question:

                Question: %s

                The answer must contain:
                - situation
                - task
                - action
                - result with measurable impact

                Return ONLY the answer as clear formatted plain text.
                """.formatted(question);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are an expert software engineer."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(baseUrl + "/chat/completions",
                        new HttpEntity<>(body, headers),
                        String.class);

        String content = objectMapper.readTree(responseEntity.getBody())
                .path("choices").get(0)
                .path("message").path("content").asText();

        return content;
    }
}
