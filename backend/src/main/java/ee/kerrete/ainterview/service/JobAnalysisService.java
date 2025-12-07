package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.RoadmapTask;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.RoadmapTaskRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
        if (apiKey == null || apiKey.isBlank()) {
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
            JobAnalysisResponse analysis =
                    objectMapper.readValue(content, JobAnalysisResponse.class);

            // Väike sanity-check
            applyDefaults(analysis, request);
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
                  "matchScore": number,                 // match score between 0.0 and 1.0 (or 0-100 if you prefer percentage)
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
        double score = estimateScoreHeuristically(request);

        JobAnalysisResponse res = new JobAnalysisResponse();
        res.setMatchScore(score);

        res.setStrengths(List.of(
                "Sul on juba kogemus Java ja Spring ökosüsteemiga.",
                "Oled maininud meeskonnatöö ja suhtlemisoskusi, mis on töökuulutuse nõuetes."
        ));

        res.setWeaknesses(List.of(
                "Pilvekeskkonna praktilise kogemuse osas on vähe detaile.",
                "Automatiseeritud testimise ja CI/CD tööriistade kasutus ei ole selgelt kirjeldatud."
        ));

        // Väga lihtne pseudo-analüüs – otsime märksõnu töökuulutusest, mida CV-s ei ole
        res.setMissingSkills(List.of(
                "Näide: React või muu kaasaegne front-end raamistik",
                "Näide: pilveplatvorm (AWS / GCP / Azure)",
                "Näide: CI/CD vahendid (GitLab CI, GitHub Actions vms)"
        ));

        res.setRoadmap(List.of(
                "Vali 1–2 pannkook-projekti, kus kasutad töökuulutuses korduvaid tehnoloogiaid.",
                "Ehita väike lõpuprojekt, kus ühendad backend'i (Java / Spring) ja frontendi.",
                "Lisa projektid oma GitHubi ja link CV-sse.",
                "Harjuta tehniliste küsimuste vastuseid just puuduolevate oskuste osas."
        ));

        res.setSuggestedImprovements("Lisa CV-sse konkreetsemad tehnoloogiad ja tööriistad, "
                + "tõsta esile mõõdetavad tulemused ning too avalehele oskused, mis kattuvad töökuulutusega.");
        res.setSummary("Lihtne fallback-analüüs: sobivus hinnanguliselt "
                + Math.round(score * 100) + "%. Täpsem analüüs eeldab OpenAI ühendust.");

        return res;
    }

    /**
     * Väga lihtne heuristika: kui CV tekst on pikk ja sarnane töökuulutusega → kõrgem skoor.
     */
    private double estimateScoreHeuristically(JobAnalysisRequest request) {
        String cv = request.getCvText() != null ? request.getCvText() : "";
        String jd = request.getJobDescription() != null ? request.getJobDescription() : "";

        if (cv.isBlank() || jd.isBlank()) {
            return 0.3;
        }

        int cvLen = cv.length();
        int jdLen = jd.length();
        double ratio = Math.min((double) cvLen / (double) jdLen, (double) jdLen / (double) cvLen);
        // ratio 0..1 – teeme sellest umbkaudse skoori
        double base = 0.4 + ratio * 0.5; // ~0.4–0.9
        return Math.max(0.1, Math.min(0.95, base));
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

    private void persistRoadmapTasks(String email, List<String> roadmapSteps) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (roadmapSteps == null || roadmapSteps.isEmpty()) {
            return;
        }

        roadmapTaskRepository.deleteByEmail(email);

        int index = 1;
        for (String step : roadmapSteps) {
            if (step == null || step.isBlank()) {
                continue;
            }
            RoadmapTask task = RoadmapTask.builder()
                    .email(email)
                    .taskKey("step-" + index)
                    .title("Step " + index)
                    .description(step.trim())
                    .completed(false)
                    .dayNumber(index)
                    .orderIndex(index)
                    .build();
            roadmapTaskRepository.save(task);
            index++;
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
                    .aiScore(response.getMatchScore() != null ? response.getMatchScore().intValue() : null)
                    .matchScore(normalizeScore(response.getMatchScore()))
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
        Double normalized = normalizeScore(response.getMatchScore());
        progress.setLastMatchScore(normalized);
        progress.setLastMatchSummary(response.getSummary());
        progress.setLastUpdated(LocalDateTime.now());
        trainingProgressRepository.save(progress);
    }

    private Double normalizeScore(Double matchScore) {
        if (matchScore == null) return null;
        if (matchScore <= 1) {
            return matchScore * 100;
        }
        return matchScore;
    }

    private String writeSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value != null ? value.toString() : null;
        }
    }
}
