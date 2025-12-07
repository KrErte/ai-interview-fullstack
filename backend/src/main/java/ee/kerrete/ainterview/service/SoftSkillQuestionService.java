package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisRequest;
import ee.kerrete.ainterview.dto.AdaptiveAnalysisResponse;
import ee.kerrete.ainterview.dto.SoftSkillQuestionRequest;
import ee.kerrete.ainterview.dto.SoftSkillQuestionResponse;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Soft-skill treener:
 *  1) kui on olemas eelmine Q/A -> hindab selle AI-ga + salvestab TrainingTask tabelisse
 *  2) küsib OpenAI-lt uue soft-skill küsimuse valitud roadmapi teemal
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillQuestionService {

    private final ObjectMapper objectMapper;
    private final AiAdaptiveService aiAdaptiveService;
    private final TrainingTaskRepository trainingTaskRepository;

    @Value("${openai.api-key:${OPENAI_API_KEY:dummy-openai-key}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Põhimeetod, mida controller kutsub.
     */
    public SoftSkillQuestionResponse generateQuestion(SoftSkillQuestionRequest request) {
        Integer score = null;
        String coachFeedback = null;
        String weakestSkill = null;

        boolean hasPrev =
                notBlank(request.getPreviousQuestion()) &&
                        notBlank(request.getPreviousAnswer());

        // 1) Kui on eelmine vastus, hinda see AI-ga ja salvesta DB-sse
        if (hasPrev) {
            try {
                AdaptiveAnalysisResponse analysis =
                        aiAdaptiveService.analyzeAnswer(
                                AdaptiveAnalysisRequest.builder()
                                        .email(request.getEmail())
                                        .roadmapItemId(request.getRoadmapKey())
                                        .question(request.getPreviousQuestion())
                                        .answer(request.getPreviousAnswer())
                                        .currentSkillSnapshot(null)
                                        .build()
                        );

                coachFeedback = analysis.getFeedback();
                weakestSkill = analysis.getWeakestSkill();

                Map<String, Integer> scores = analysis.getUpdatedSkills();
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.values().stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0.0);
                    score = (int) Math.round(avg);
                }

                saveTrainingTask(request, score);
            } catch (Exception e) {
                log.error("Viga vastuse analüüsimisel / DB salvestamisel", e);
            }
        }

        // 2) Küsi OpenAI-lt järgmine küsimus valitud roadmapi kohta
        SoftSkillQuestionResponse nextQ = callOpenAiForNextQuestion(request);

        // 3) Lisa AI coach’i info vastusele
        return SoftSkillQuestionResponse.builder()
                .questionText(nextQ.getQuestionText())
                .difficulty(nextQ.getDifficulty())
                .fallback(nextQ.isFallback())
                .score(score)
                .coachFeedback(coachFeedback)
                .weakestSkill(weakestSkill)
                .build();
    }

    /**
     * Salvestab ühe TrainingTask kirje eelmise Q/A kohta.
     */
    private void saveTrainingTask(SoftSkillQuestionRequest request, Integer score) {
        if (!notBlank(request.getEmail()) || !notBlank(request.getPreviousQuestion())) {
            return; // anonüümne või liiga pooleli – ei salvesta
        }

        String taskKey = buildTaskKey(request);

        TrainingTask task = TrainingTask.builder()
                .email(request.getEmail())
                .taskKey(taskKey)
                .skillKey(request.getRoadmapKey())
                .question(request.getPreviousQuestion())
                .answer(request.getPreviousAnswer())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .completed(true)
                .score(score != null ? score : 0)
                .build();

        trainingTaskRepository.save(task);
    }

    /**
     * Teeb stabiilse key, et sama küsimuse vastused grupeeruksid:
     * nt "conflict_management_ai_123456"
     */
    private String buildTaskKey(SoftSkillQuestionRequest request) {
        String base = nullToEmpty(request.getRoadmapKey());
        if (base.isEmpty()) {
            base = "softskill";
        }
        String q = nullToEmpty(request.getPreviousQuestion());
        int hash = q.isEmpty() ? 0 : Math.abs(q.hashCode());
        return base + "_ai_" + hash;
    }

    // --- OpenAI järgmise küsimuse jaoks ---

    private SoftSkillQuestionResponse callOpenAiForNextQuestion(SoftSkillQuestionRequest request) {
        try {
            String url = chatCompletionsUrl();

            String systemPrompt = buildSystemPrompt(request.getRoadmapKey());
            String userPrompt = buildUserPrompt(request);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            body.put("response_format", responseFormat);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                    "role", "system",
                    "content", systemPrompt
            ));
            messages.add(Map.of(
                    "role", "user",
                    "content", userPrompt
            ));

            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 512);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String responseBody = Optional.ofNullable(response.getBody()).orElse("");

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenAI vastuses puudub choices massiiv");
            }

            JsonNode messageNode = choices.get(0).path("message");
            String content = messageNode.path("content").asText();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("OpenAI message.content on tühi");
            }

            // content on JSON string – parsime
            JsonNode contentJson = objectMapper.readTree(content);

            String questionText = contentJson.path("questionText").asText(null);
            if (questionText == null || questionText.isBlank()) {
                questionText = contentJson.path("question").asText(null);
            }
            String difficulty = contentJson.path("difficulty").asText("medium");

            if (questionText == null || questionText.isBlank()) {
                return SoftSkillQuestionResponse.builder()
                        .questionText(fallbackQuestion(request.getRoadmapKey()))
                        .difficulty("medium")
                        .fallback(true)
                        .build();
            }

            return SoftSkillQuestionResponse.builder()
                    .questionText(questionText.trim())
                    .difficulty(difficulty != null && !difficulty.isBlank() ? difficulty : "medium")
                    .fallback(false)
                    .build();

        } catch (Exception e) {
            log.error("SoftSkillQuestionService.callOpenAiForNextQuestion error", e);
            return SoftSkillQuestionResponse.builder()
                    .questionText(fallbackQuestion(request.getRoadmapKey()))
                    .difficulty("medium")
                    .fallback(true)
                    .build();
        }
    }

    private String chatCompletionsUrl() {
        return baseUrl.endsWith("/")
                ? baseUrl + "chat/completions"
                : baseUrl + "/chat/completions";
    }

    private String buildSystemPrompt(String roadmapKey) {
        String topicDescription;
        switch (nullToEmpty(roadmapKey)) {
            case "technical_initiative" ->
                    topicDescription = "tehnilise initsiatiivi võtmine, ownership, lahenduste välja pakkumine";
            case "clear_communication" ->
                    topicDescription = "selge kommunikatsioon, ootuste seadmine, keeruliste ideede selgitamine";
            case "conflict_management" ->
                    topicDescription = "tiimikonfliktide rahulik ja konstruktiivne lahendamine";
            case "ownership" ->
                    topicDescription = "vastutuse võtmine, probleemidele omanikuks olemine, bugfixi lõpuni vedamine";
            default ->
                    topicDescription = "üldised koostöö- ja kommunikatsioonioskused (soft skills)";
        }

        return """
                Sa oled kogenud engineering manager ja soft-skill coach.
                Fookusteema: %s

                Sinu ülesanne: genereeri tarkvaraarendajale või tech leadile suunatud käitumuslikke "soft skills" küsimusi.
                Küsimused on eesti keeles, konkreetsed ja põhinevad päris tööolukordadel.
                Esita korraga ainult ÜKS küsimus.
                Väljund peab alati olema JSON kujul (response_format=json_object).
                """.formatted(topicDescription);
    }

    private String buildUserPrompt(SoftSkillQuestionRequest request) {
        boolean isFirstQuestion =
                !notBlank(request.getPreviousQuestion()) &&
                        !notBlank(request.getPreviousAnswer());

        StringBuilder historyBuilder = new StringBuilder();
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            historyBuilder.append("Senine treeningajalugu:\n");
            for (SoftSkillQuestionRequest.HistoryTurn turn : request.getHistory()) {
                historyBuilder.append("Küsimus: ")
                        .append(nullToEmpty(turn.getQuestion()))
                        .append("\nVastus: ")
                        .append(nullToEmpty(turn.getAnswer()))
                        .append("\n\n");
            }
        }

        if (isFirstQuestion) {
            return """
                    Kasutaja alustab uut treeningsessiooni soft-skilli teemal "%s".
                    Genereeri ÜKS tugev käitumuslik intervjueerimisküsimus, mis aitab hinnata ja arendada seda oskust.
                    Küsimus olgu:
                    - eesti keeles
                    - suunatud tarkvaraarendajale / tech leadile
                    - praktiline, palub kirjeldada päris olukorda.

                    Vasta ainult JSON-iga:
                    {
                      "questionText": "...",
                      "difficulty": "easy|medium|hard"
                    }
                    """.formatted(nullToEmpty(request.getRoadmapKey()));
        } else {
            return """
                    Jätkame treeningsessiooni soft-skilli teemal "%s".
                    %s
                    Viimane küsimus oli:
                    %s

                    Kasutaja vastus oli:
                    %s

                    Sinu ülesanne:
                    1) Mõtle, mis oleks loogiline JÄRGMINE küsimus, mis viib teema sügavamale.
                    2) Küsimus peab toetama sama soft-skill'i arengut.
                    3) Küsi korraga ainult ÜKS asi.

                    Vasta ainult JSON-iga:
                    {
                      "questionText": "...",
                      "difficulty": "easy|medium|hard"
                    }
                    """.formatted(
                    nullToEmpty(request.getRoadmapKey()),
                    historyBuilder.toString(),
                    nullToEmpty(request.getPreviousQuestion()),
                    nullToEmpty(request.getPreviousAnswer())
            );
        }
    }

    private String fallbackQuestion(String roadmapKey) {
        switch (nullToEmpty(roadmapKey)) {
            case "conflict_management":
                return "Kirjelda üht hiljutist olukorda, kus sinu tiimis tekkis konflikt. Kuidas sa ise selles olukorras käitusid ja mida tegid, et olukorda lahendada?";
            case "clear_communication":
                return "Räägi olukorrast, kus pidid keerulise tehnilise teema lahti seletama äripoolele või mitte-tehnilisele kolleegile. Kuidas sa seda tegid?";
            case "technical_initiative":
                return "Kirjelda olukorda, kus võtsid ise initsiatiivi tehnilise probleemi lahendamisel või uue lahenduse pakkumisel. Mis täpselt juhtus?";
            case "ownership":
                return "Räägi olukorrast, kus mingi probleem või bug jäi justkui \"õhku rippuma\". Kuidas sa võtsid sellele olukorrale omanikuks olemise rolli?";
            default:
                return "Kirjelda üht olukorda oma töös, kus sinu soft-skills (kommunikatsioon, koostöö, konfliktide lahendamine) mängisid olulist rolli.";
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
