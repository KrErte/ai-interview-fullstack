package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.config.OpenAiProperties;
import ee.kerrete.ainterview.model.Question;
import lombok.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Ühtne OpenAI kliendi wrapper.
 * - createChatCompletion(...)  → toor-JSON (kasutatakse LearningPlan / SkillPlan jaoks)
 * - evaluateAnswer(...)        → tagastab JSON stringi AnswerEvaluation jaoks
 * - generateQuestionsFromCv(...)→ tagastab List<Question>
 * - generateSkillBoosterPlan(...)→ tagastab JSON stringi SkillPlanResponse jaoks
 * - complete(...)              → lihtne "üks prompt" helper (kasutab StarAnswerController jms)
 */
@Service
public class OpenAiClient {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiClient(OpenAiProperties properties) {
        this.properties = properties;
    }

    /* =======================================================================
       PÕHI-MEETOD: madala taseme chat completion
       ======================================================================= */

    private String callChat(List<ChatMessage> messages,
                            Integer maxTokens,
                            Double temperature) {

        String url = properties.getBaseUrl() + "/chat/completions";

        OpenAiRequest body = OpenAiRequest.builder()
                .model(properties.getModel())
                .messages(messages)
                .maxTokens(maxTokens != null ? maxTokens : 2048)
                .temperature(temperature != null ? temperature : 0.7)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        HttpEntity<OpenAiRequest> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<OpenAiResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, OpenAiResponse.class);

            if (response.getBody() == null ||
                    response.getBody().choices == null ||
                    response.getBody().choices.isEmpty() ||
                    response.getBody().choices.get(0).message == null) {
                return "AI ei tagastanud vastust.";
            }

            return response.getBody().choices.get(0).message.content.trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "OpenAI päring ebaõnnestus: " + e.getMessage();
        }
    }

    /* =======================================================================
       LIHTNE "ÜKS PROMPT" meetod – kasutavad uued kohad (nt STAR vastus)
       ======================================================================= */

    public String complete(String prompt) {
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", "You are a helpful AI assistant."),
                new ChatMessage("user", prompt)
        );
        return callChat(messages, 2048, 0.8);
    }

    /* =======================================================================
       1) createChatCompletion – CandidatePlanService kasutab
       ======================================================================= */

    /**
     * Kasutatakse LearningPlani genereerimiseks.
     * CandidatePlanService annab ette nii systemPrompti kui userPrompti,
     * meie ainult edastame need OpenAI-le.
     */
    public String createChatCompletion(String systemPrompt, String userPrompt) {
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );
        return callChat(messages, 2048, 0.7);
    }

    /* =======================================================================
       2) evaluateAnswer – EvaluationService kasutab (AnswerEvaluation JSON)
       ======================================================================= */

    /**
     * Genereerib vastusehindamise JSON-i.
     * Tagastab JSON stringi, mida AnswerEvaluation.fromJson(...) parsetakse.
     */
    public String evaluateAnswer(String question, String answer) {
        String systemPrompt = """
                You are a senior software engineering interviewer.
                Evaluate candidate answers very practically.

                Return ONLY valid JSON, no markdown, no explanation.
                JSON structure:
                {
                  "score": 0-100,
                  "strengths": "short paragraph in Estonian",
                  "weaknesses": "short paragraph in Estonian",
                  "suggestions": "short practical tips in Estonian"
                }
                """;

        String userPrompt = """
                Küsimus:
                %s

                Kandidaadi vastus:
                %s
                """.formatted(question, answer);

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        return callChat(messages, 800, 0.7);
    }

    /* =======================================================================
       3) generateQuestionsFromCv – QuestionService & InterviewQuestionService
       ======================================================================= */

    /**
     * Genereerib tehnilised + soft-skill küsimused CV põhjal.
     * Tagastab List<Question>, mille QuestionService juba ootab.
     */
    public List<Question> generateQuestionsFromCv(String cvText,
                                                  int technicalCount,
                                                  int softCount) {

        String systemPrompt = """
                You are a senior software engineer and technical interviewer.
                Generate realistic interview questions based on a candidate CV text.

                Return ONLY valid JSON array (no markdown), example:
                [
                  {
                    "id": "1",
                    "question": "Küsimus tekstina",
                    "difficulty": "TECH" or "SOFT",
                    "category": "JUNIOR" or "MID" or "SENIOR"
                  }
                ]
                """;

        String userPrompt = """
                Candidate CV:
                %s

                Generate %d technical questions (difficulty TECH)
                and %d soft-skill questions (difficulty SOFT).
                """.formatted(cvText, technicalCount, softCount);

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        String json = callChat(messages, 1200, 0.8);

        try {
            Question[] arr = objectMapper.readValue(json, Question[].class);
            List<Question> result = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    Question q = arr[i];
                    // kui ID puudub, pane lihtsalt jrk nr
                    if (q.getId() == null || q.getId().isBlank()) {
                        q.setId(String.valueOf(i + 1));
                    }
                    result.add(q);
                }
            }
            if (result.isEmpty()) {
                // fallback – vähemalt üks küsimus
                result.add(new Question("1",
                        "Kirjelda üht huvitavat projekti oma CV-st.",
                        "SOFT",
                        "MID"));
            }
            return result;
        } catch (Exception e) {
            // kui JSON tuksi läheb, tagastame fallbacki
            e.printStackTrace();
            List<Question> fallback = new ArrayList<>();
            fallback.add(new Question("1", "Kirjelda REST API mõistet.", "TECH", "JUNIOR"));
            fallback.add(new Question("2", "Kuidas lahendaksid konflikti tiimis?", "SOFT", "MID"));
            return fallback;
        }
    }

    /* =======================================================================
       4) generateSkillBoosterPlan – SkillPlanService kasutab
       ======================================================================= */

    /**
     * Genereerib 21-päevase oskuse arendamise plaani JSON-i
     * (SkillPlanResponse), mille SkillPlanService ise ObjectMapperiga parsetab.
     */
    public String generateSkillBoosterPlan(String jobMatcherSummary,
                                           List<String> focusSkills) {

        String systemPrompt = """
                You are a senior career coach for software engineers.
                Create a concise 21-day learning plan for selected skills.

                Return ONLY valid JSON (no markdown) with structure:
                {
                  "overallGoal": "one sentence in Estonian",
                  "days": [
                    {
                      "dayNumber": 1,
                      "title": "short title",
                      "description": "2-3 sentences of explanation (Estonian)",
                      "practiceTask": "1 concrete practice task (Estonian)"
                    }
                  ]
                }
                """;

        String skills = String.join(", ", focusSkills == null ? List.of() : focusSkills);

        String userPrompt = """
                Job Matcheri AI kokkuvõte:
                %s

                Fookusoskused, mida kandidaat soovib arendada:
                %s

                Koosta 21-päevane arenguplaan (days[0..20]).
                """.formatted(jobMatcherSummary, skills);

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );

        return callChat(messages, 1800, 0.7);
    }

    /* =======================================================================
       DTO-d OpenAI päringu/ vastuse jaoks
       ======================================================================= */

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class OpenAiRequest {
        private String model;
        private List<ChatMessage> messages;

        @JsonProperty("max_tokens")
        private Integer maxTokens;

        private Double temperature;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    static class ChatMessage {
        private String role;
        private String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiResponse {
        public List<Choice> choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        public ChatMessage message;
    }
}
