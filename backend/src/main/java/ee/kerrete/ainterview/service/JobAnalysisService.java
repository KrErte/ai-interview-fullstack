package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ee.kerrete.ainterview.ai.OpenAiKey;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.ProductEventType;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAnalysisService {

    private final ObjectMapper objectMapper;
    private final RoadmapTaskRepository roadmapTaskRepository;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    /** product analytics: written by ProductEventService after commit, see ProductEventOccurred */
    private final ApplicationEventPublisher events;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    /**
     * Põhimeetod, mida controller kutsub.
     */
    public JobAnalysisResponse analyze(JobAnalysisRequest request) {
        JobAnalysisResponse response;

        // Kui OpenAI võti puudub, ära üldse ürita API-t kutsuda – tee fallback
        if (!OpenAiKey.isConfigured(apiKey)) {
            log.warn("OPENAI_API_KEY puudub – kasutan fallback JobAnalysisService loogikat.");
            response = fallbackAnalyze(request);
            persistRoadmapTasks(request.getEmail(), response.getRoadmap());
            persistSession(request, response);
            updateTrainingProgress(request.getEmail(), response);
            return response;
        }

        try {
            String prompt = buildPrompt(request);

            RestTemplate restTemplate = new RestTemplate();

            // OpenAI chat/completions payload
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.25,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are an AI assistant that analyzes a candidate CV against a job description for an AI Interview Mentor app. " +
                                            "ALWAYS answer in compact JSON matching the given schema. Do not add any other text."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = baseUrl.endsWith("/")
                    ? baseUrl + "chat/completions"
                    : baseUrl + "/chat/completions";

            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful() ||
                    responseEntity.getBody() == null) {
                log.warn("OpenAI vastus ei olnud 2xx: {}", responseEntity.getStatusCode());
                response = fallbackAnalyze(request);
                persistRoadmapTasks(request.getEmail(), response.getRoadmap());
                return response;
            }

            String raw = responseEntity.getBody();

            // Loe välja choices[0].message.content
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("OpenAI vastuses puudub choices massiiv: {}", raw);
                response = fallbackAnalyze(request);
                persistRoadmapTasks(request.getEmail(), response.getRoadmap());
                persistSession(request, response);
                updateTrainingProgress(request.getEmail(), response);
                return response;
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText("");
            if (content.isBlank()) {
                log.warn("OpenAI vastuse content tühi: {}", raw);
                response = fallbackAnalyze(request);
                persistRoadmapTasks(request.getEmail(), response.getRoadmap());
                persistSession(request, response);
                updateTrainingProgress(request.getEmail(), response);
                return response;
            }

            // GPT peab tagastama puhta JSONi – proovime otse JobAnalysisResponse peale map'ida
            JsonNode tree = objectMapper.readTree(content);
            // FP-20: read the raw score ourselves; binding 0.82 straight into the Integer field would truncate it to 0
            Double rawScore = null;
            if (tree instanceof ObjectNode obj) {
                JsonNode scoreNode = obj.hasNonNull("matchScore") ? obj.get("matchScore") : obj.get("score");
                if (scoreNode != null && scoreNode.isNumber()) {
                    rawScore = scoreNode.asDouble();
                }
                obj.remove("matchScore");
                obj.remove("score");
            }
            JobAnalysisResponse analysis = objectMapper.treeToValue(tree, JobAnalysisResponse.class);
            if (analysis == null) {
                throw new IllegalStateException("OpenAI content is not a JSON object");
            }
            analysis.setMatchScore(normalizeScore(rawScore));

            // Väike sanity-check
            applyDefaults(analysis, request);
            analysis.setFallback(false);
            response = analysis;

        } catch (Exception e) {
            log.error("JobAnalysisService OpenAI-kõne ebaõnnestus, kasutan fallbacki", e);
            response = fallbackAnalyze(request);
        }

        persistRoadmapTasks(request.getEmail(), response.getRoadmap());
        persistSession(request, response);
        updateTrainingProgress(request.getEmail(), response);
        return response;
    }

    /**
     * Prompt, mida saadame GPT-le – kirjeldame skeemi selgelt.
     */
    private String buildPrompt(JobAnalysisRequest request) {
        String cv = request.getCvText() != null ? request.getCvText() : "";
        String jd = request.getJobDescription() != null ? request.getJobDescription() : "";

        return """
                Analyze the following candidate CV against the job description.

                CV (plain text):
                %s

                JOB DESCRIPTION:
                %s

                Return STRICT JSON with this exact structure (field names must match):

                {
                  "matchScore": number,                 // match score as an integer from 0 to 100 (percent)
                  "strengths": ["string"],              // candidate strengths vs job description
                  "weaknesses": ["string"],             // weaknesses or risks vs job description
                  "missingSkills": ["string"],          // list of key skills the candidate is missing vs job
                  "roadmap": ["string"],                // ordered learning roadmap (Estonian)
                  "suggestedImprovements": "string",    // concise text with improvement suggestions
                  "summary": "string"                   // 1-3 sentence summary in Estonian
                }

                Do not wrap JSON in markdown. No explanation, ONLY the JSON object.
                """.formatted(cv, jd);
    }

    /**
     * Fallback, kui OpenAI ei ole saadaval – annab siiski midagi mõistlikku.
     */
    private JobAnalysisResponse fallbackAnalyze(JobAnalysisRequest request) {
        int score = estimateScoreHeuristically(request);

        JobAnalysisResponse res = new JobAnalysisResponse();
        res.setMatchScore(score);
        res.setFallback(true);

        // FP-20: the heuristic only compares text lengths, so it cannot know strengths, gaps, a roadmap
        // or advice. We never show invented content: all of these stay empty in the fallback.
        res.setStrengths(Collections.emptyList());
        res.setWeaknesses(Collections.emptyList());
        res.setMissingSkills(Collections.emptyList());
        res.setRoadmap(Collections.emptyList());
        res.setSuggestedImprovements("");
        // factual: what the number is and why there is nothing more
        res.setSummary("Lihtne fallback-analüüs: sobivus hinnanguliselt "
                + score + "%. Täpsem analüüs eeldab OpenAI ühendust.");

        return res;
    }

    /**
     * Väga lihtne heuristika: kui CV tekst on pikk ja sarnane töökuulutusega → kõrgem skoor.
     * Returns a whole number on the 0–100 scale (FP-20).
     */
    private int estimateScoreHeuristically(JobAnalysisRequest request) {
        String cv = request.getCvText() != null ? request.getCvText() : "";
        String jd = request.getJobDescription() != null ? request.getJobDescription() : "";

        if (cv.isBlank() || jd.isBlank()) {
            return 30;
        }

        int cvLen = cv.length();
        int jdLen = jd.length();
        double ratio = Math.min((double) cvLen / (double) jdLen, (double) jdLen / (double) cvLen);
        // ratio 0..1 – teeme sellest umbkaudse skoori
        double base = 40 + ratio * 50; // ~40–90
        return (int) Math.round(Math.max(10, Math.min(95, base)));
    }

    private void applyDefaults(JobAnalysisResponse analysis, JobAnalysisRequest request) {
        if (analysis.getMatchScore() == null) {
            analysis.setMatchScore(estimateScoreHeuristically(request));
        }
        if (analysis.getStrengths() == null) {
            analysis.setStrengths(Collections.emptyList());
        }
        if (analysis.getWeaknesses() == null) {
            analysis.setWeaknesses(Collections.emptyList());
        }
        if (analysis.getMissingSkills() == null) {
            analysis.setMissingSkills(Collections.emptyList());
        }
        if (analysis.getRoadmap() == null) {
            analysis.setRoadmap(Collections.emptyList());
        }
        if (analysis.getSuggestedImprovements() == null) {
            analysis.setSuggestedImprovements("");
        }
    }

    /**
     * Creates the initial roadmap from an analysis, but only when the user has no roadmap yet.
     * A new analysis never overwrites an existing plan and never deletes (completed) tasks;
     * replacing a plan is an explicit action via RoadmapService.savePlan.
     * Tasks are written with a single saveAll call, which runs in one transaction.
     */
    private void persistRoadmapTasks(String email, List<String> roadmapSteps) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (roadmapSteps == null || roadmapSteps.isEmpty()) {
            return;
        }
        if (roadmapTaskRepository.countByEmail(email) > 0) {
            log.info("Roadmap already exists for user, not overwriting it with the new analysis");
            return;
        }

        List<RoadmapTask> tasks = new ArrayList<>();
        int index = 1;
        for (String step : roadmapSteps) {
            if (step == null || step.isBlank()) {
                continue;
            }
            tasks.add(RoadmapTask.builder()
                    .email(email)
                    .taskKey("step-" + index)
                    .title("Step " + index)
                    .description(step.trim())
                    .completed(false)
                    .dayNumber(index)
                    .orderIndex(index)
                    .build());
            index++;
        }
        if (!tasks.isEmpty()) {
            roadmapTaskRepository.saveAll(tasks);
            events.publishEvent(new ProductEventOccurred(email, ProductEventType.PLAN_GENERATED,
                    Map.of("source", "job_analysis", "taskCount", tasks.size())));
        }
    }

    private void persistSession(JobAnalysisRequest request, JobAnalysisResponse response) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return;
        }
        try {
            JobAnalysisSession session = JobAnalysisSession.builder()
                    .email(request.getEmail())
                    .jobTitle("")
                    .jobDescription(request.getJobDescription())
                    .analysisResult(objectMapper.writeValueAsString(response))
                    .strengthsJson(writeSafe(response.getStrengths()))
                    .weaknessesJson(writeSafe(response.getWeaknesses()))
                    .missingSkillsJson(writeSafe(response.getMissingSkills()))
                    .roadmapJson(writeSafe(response.getRoadmap()))
                    .suggestedImprovementsJson(writeSafe(response.getSuggestedImprovements()))
                    .aiSummary(response.getSummary())
                    // FP-20: the response score is already normalized (integer 0-100); persist the same value
                    .aiScore(response.getMatchScore())
                    .matchScore(asDouble(response.getMatchScore()))
                    .summary(response.getSummary())
                    .createdAt(LocalDateTime.now())
                    .build();
            jobAnalysisSessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Persisting job analysis session failed", e);
        }
    }

    private void updateTrainingProgress(String email, JobAnalysisResponse response) {
        if (email == null || email.isBlank()) {
            return;
        }
        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(email)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastUpdated(LocalDateTime.now())
                        .build());

        progress.setTotalJobAnalyses(progress.getTotalJobAnalyses() + 1);
        progress.setLastMatchScore(asDouble(response.getMatchScore()));
        progress.setLastMatchSummary(response.getSummary());
        progress.setLastUpdated(LocalDateTime.now());
        trainingProgressRepository.save(progress);
    }

    /**
     * FP-20 match score contract: integer 0–100. ONLY values below 1 are fractions (0.82 -> 82); 1 stays 1
     * (1%), since the model is asked for 0–100. Then clamp to 0–100 and round half up. Idempotent on its own
     * output. null / NaN / infinite -> null.
     */
    static Integer normalizeScore(Double matchScore) {
        if (matchScore == null || matchScore.isNaN() || matchScore.isInfinite()) return null;
        double value = matchScore < 1 ? matchScore * 100 : matchScore;
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }

    private static Double asDouble(Integer score) {
        return score == null ? null : score.doubleValue();
    }

    private String writeSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value != null ? value.toString() : null;
        }
    }
}
