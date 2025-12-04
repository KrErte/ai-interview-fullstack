package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.JobAnalysisRequest;
import ee.kerrete.ainterview.dto.JobAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAnalysisService {

    private final ObjectMapper objectMapper;

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
        // Kui OpenAI võti puudub, ära üldse ürita API-t kutsuda – tee fallback
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY puudub – kasutan fallback JobAnalysisService loogikat.");
            return fallbackAnalyze(request);
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
                return fallbackAnalyze(request);
            }

            String raw = responseEntity.getBody();

            // Loe välja choices[0].message.content
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("OpenAI vastuses puudub choices massiiv: {}", raw);
                return fallbackAnalyze(request);
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText("");
            if (content.isBlank()) {
                log.warn("OpenAI vastuse content tühi: {}", raw);
                return fallbackAnalyze(request);
            }

            // GPT peab tagastama puhta JSONi – proovime otse JobAnalysisResponse peale map'ida
            JobAnalysisResponse analysis =
                    objectMapper.readValue(content, JobAnalysisResponse.class);

            // Väike sanity-check
            if (analysis.getScore() == null) {
                analysis.setScore(estimateScoreHeuristically(request));
            }
            if (analysis.getMissingSkills() == null) {
                analysis.setMissingSkills(Collections.emptyList());
            }
            if (analysis.getRoadmap() == null) {
                analysis.setRoadmap(Collections.emptyList());
            }
            if (analysis.getSuggestedImprovements() == null) {
                analysis.setSuggestedImprovements(Collections.emptyList());
            }

            return analysis;

        } catch (Exception e) {
            log.error("JobAnalysisService OpenAI-kõne ebaõnnestus, kasutan fallbacki", e);
            return fallbackAnalyze(request);
        }
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
                  "score": number,                // match score between 0.0 and 1.0
                  "summary": "string",            // 1-3 sentence summary in Estonian
                  "missingSkills": ["string"],    // list of key skills the candidate is missing vs job
                  "roadmap": ["string"],          // ordered learning roadmap (Estonian)
                  "suggestedImprovements": ["string"] // specific CV / skill improvement tips
                }

                Do not wrap JSON in markdown. No explanation, ONLY the JSON object.
                """.formatted(cv, jd);
    }

    /**
     * Fallback, kui OpenAI ei ole saadaval – annab siiski midagi mõistlikku.
     */
    private JobAnalysisResponse fallbackAnalyze(JobAnalysisRequest request) {
        String cv = request.getCvText() != null ? request.getCvText() : "";
        String jd = request.getJobDescription() != null ? request.getJobDescription() : "";

        double score = estimateScoreHeuristically(request);

        JobAnalysisResponse res = new JobAnalysisResponse();
        res.setScore(score);

        res.setSummary("Lihtne fallback-analüüs: sobivus hinnanguliselt "
                + Math.round(score * 100) + "%. Täpsem analüüs eeldab OpenAI ühendust.");

        // Väga lihtne pseudo-analüüs – otsime märksõnu töökuulutusest, mida CV-s ei ole
        res.setMissingSkills(List.of(
                "Näide: React või muu kaasaegne front-end raamistik",
                "Näide: pilveplatvorm (AWS / GCP / Azure)",
                "Näide: CI/CD vahendid (GitLab CI, GitHub Actions vms)"
        ));

        res.setRoadmap(List.of(
                "Vali 1–2 pannkook-projekti, kus kasutad neid tehnoloogiaid, mis töökuulutuses korduvad.",
                "Ehita väike lõpuprojekt, kus ühendad backend'i (Java / Spring) ja frontendi.",
                "Lisa projektid oma GitHubi ja link CV-sse.",
                "Harjuta tehniliste küsimuste vastuseid just nende puuduolevate oskuste osas."
        ));

        res.setSuggestedImprovements(List.of(
                "Lisa CV-sse konkreetsemad tehnoloogiad ja tööriistad, mida oled kasutanud.",
                "Too välja mõõdetavad tulemused (nt jõudluse paranemine, automatiseerimise võit).",
                "Struktureeri CV nii, et kõige olulisemad oskused, mis kuulutuses mainitud, oleksid ülalpool."
        ));

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
}
