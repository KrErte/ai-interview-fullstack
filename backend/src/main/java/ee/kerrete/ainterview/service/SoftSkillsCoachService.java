package ee.kerrete.ainterview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.dto.CoachAnswerRequest;
import ee.kerrete.ainterview.dto.CoachAnswerResponse;
import ee.kerrete.ainterview.dto.CoachStateResponse;
import ee.kerrete.ainterview.dto.TrainingProgressResponse;
import ee.kerrete.ainterview.model.JobAnalysisSession;
import ee.kerrete.ainterview.model.TrainingProgress;
import ee.kerrete.ainterview.model.TrainingStatus;
import ee.kerrete.ainterview.model.TrainingTask;
import ee.kerrete.ainterview.repository.JobAnalysisSessionRepository;
import ee.kerrete.ainterview.repository.TrainingProgressRepository;
import ee.kerrete.ainterview.repository.TrainingTaskRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftSkillsCoachService {

    private final ObjectMapper objectMapper;
    private final TrainingTaskRepository trainingTaskRepository;
    private final TrainingProgressRepository trainingProgressRepository;
    private final SoftSkillSeedService softSkillSeedService;
    private final JobAnalysisSessionRepository jobAnalysisSessionRepository;

    @Value("${openai.api-key:${OPENAI_API_KEY:dummy-openai-key}}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public CoachStateResponse getState(String email) {
        String skillKey = resolveCurrentSkill(email);
        List<TrainingTask> history = orderedHistory(
                trainingTaskRepository.findByEmailAndSkillKeyOrderByCreatedAtDesc(email, skillKey)
        );

        int questionsAsked = history.size();
        int maxQuestions = resolveMaxQuestions(skillKey);
        boolean sessionComplete = maxQuestions > 0 && questionsAsked >= maxQuestions;

        String jobSummary = resolveJobSummary(email);
        String question = sessionComplete
                ? null
                : askForNextQuestion(skillKey, history, jobSummary, questionsAsked, maxQuestions, email);

        TrainingProgressResponse progress = recomputeProgress(email, questionsAsked, maxQuestions, false);
        String lastFeedback = history.stream()
                .filter(t -> StringUtils.hasText(t.getFeedback()))
                .findFirst()
                .map(TrainingTask::getFeedback)
                .orElse(null);

        return CoachStateResponse.builder()
                .currentSkillKey(skillKey)
                .currentQuestion(question)
                .questionsAsked(questionsAsked)
                .maxQuestions(maxQuestions)
                .tasksCompleted(progress.getCompletedTasks())
                .totalTasks(progress.getTotalTasks())
                .trainingProgressPercent(progress.getTrainingProgressPercent())
                .lastFeedback(lastFeedback)
                .build();
    }

    public CoachAnswerResponse submitAnswer(String email, CoachAnswerRequest request) {
        String skillKey = defaultSkillKey(request.getSkillKey());
        List<TrainingTask> history = orderedHistory(
                trainingTaskRepository.findByEmailAndSkillKeyOrderByCreatedAtDesc(email, skillKey)
        );

        int askedSoFar = history.size();
        int maxQuestions = resolveMaxQuestions(skillKey);
        String jobSummary = resolveJobSummary(email);

        CoachModelResult modelResult = requestFeedbackAndNext(
                email,
                skillKey,
                history,
                request.getQuestion(),
                request.getAnswer(),
                jobSummary,
                askedSoFar,
                maxQuestions
        );

        saveTrainingTask(email, skillKey, request.getQuestion(), request.getAnswer(), modelResult.getFeedback());

        int questionsAsked = askedSoFar + 1;
        boolean sessionComplete = modelResult.isSessionComplete()
                || (maxQuestions > 0 && questionsAsked >= maxQuestions);

        String nextQuestion = sessionComplete
                ? null
                : ensureNextQuestion(
                skillKey,
                modelResult.getNextQuestion(),
                questionsAsked,
                maxQuestions,
                jobSummary,
                history,
                request,
                email
        );

        TrainingProgressResponse progress = recomputeProgress(email, questionsAsked, maxQuestions, true);

        return CoachAnswerResponse.builder()
                .feedback(modelResult.getFeedback())
                .nextQuestion(nextQuestion)
                .tasksCompleted(progress.getCompletedTasks())
                .totalTasks(progress.getTotalTasks())
                .trainingProgressPercent(progress.getTrainingProgressPercent())
                .questionsAsked(questionsAsked)
                .maxQuestions(maxQuestions)
                .sessionComplete(sessionComplete)
                .lastFeedback(modelResult.getFeedback())
                .build();
    }

    private String askForNextQuestion(
            String skillKey,
            List<TrainingTask> history,
            String jobSummary,
            int questionsAsked,
            int maxQuestions,
            String email
    ) {
        try {
            String systemPrompt = buildQuestionSystemPrompt(skillKey);
            String userPrompt = buildQuestionUserPrompt(skillKey, history, jobSummary, questionsAsked, maxQuestions);

            JsonNode contentJson = callOpenAi(systemPrompt, userPrompt);
            String question = contentJson.path("questionText").asText(null);
            boolean sessionComplete = contentJson.path("sessionComplete").asBoolean(false);

            if (!sessionComplete && StringUtils.hasText(question)) {
                return question.trim();
            }
        } catch (OpenAiCoachException e) {
            throw e;
        } catch (Exception e) {
            log.error("SoftSkillsCoachService askForNextQuestion failed skillKey={} email={}", skillKey, email, e);
        }

        return softSkillSeedService.getNextSeed(skillKey, questionsAsked)
                .map(SoftSkillSeedService.SeedQuestion::getQuestionText)
                .orElseGet(() -> fallbackQuestion(skillKey));
    }

    private CoachModelResult requestFeedbackAndNext(
            String email,
            String skillKey,
            List<TrainingTask> history,
            String question,
            String answer,
            String jobSummary,
            int askedSoFar,
            int maxQuestions
    ) {
        try {
            String systemPrompt = buildCoachSystemPrompt(skillKey);
            String userPrompt = buildFeedbackPrompt(
                    skillKey,
                    history,
                    question,
                    answer,
                    jobSummary,
                    askedSoFar,
                    maxQuestions
            );

            JsonNode contentJson = callOpenAi(systemPrompt, userPrompt);

            String feedback = contentJson.path("feedback").asText("");
            String nextQuestion = contentJson.path("nextQuestion").asText(null);
            boolean sessionComplete = contentJson.path("sessionComplete").asBoolean(false);

            return CoachModelResult.builder()
                    .feedback(feedback)
                    .nextQuestion(StringUtils.hasText(nextQuestion) ? nextQuestion.trim() : null)
                    .sessionComplete(sessionComplete)
                    .build();
        } catch (OpenAiCoachException e) {
            throw e;
        } catch (Exception e) {
            log.error("SoftSkillsCoachService requestFeedbackAndNext failed skillKey={} email={}", skillKey, email, e);
            throw new OpenAiCoachException("OpenAI teenus ei vastanud.", e);
        }
    }

    private JsonNode callOpenAi(String systemPrompt, String userPrompt) {
        try {
            if (!StringUtils.hasText(apiKey)) {
                throw new OpenAiCoachException("OpenAI API võti puudub.", new IllegalStateException("openai.api-key missing"));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            body.put("temperature", 0.6);
            body.put("max_tokens", 512);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(Objects.requireNonNull(apiKey));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String base = Objects.requireNonNull(baseUrl, "openai.base-url missing");
            String url = base.endsWith("/")
                    ? base + "chat/completions"
                    : base + "/chat/completions";
            HttpMethod method = HttpMethod.POST;
            Objects.requireNonNull(url);
            Objects.requireNonNull(method);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, method, entity, String.class);

            String responseBody = Optional.ofNullable(response.getBody()).orElse("");
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("OpenAI choices missing");
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("OpenAI content empty");
            }

            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("SoftSkillsCoachService OpenAI error", e);
            throw new OpenAiCoachException("OpenAI teenus ei ole saadaval.", e);
        }
    }

    private TrainingProgressResponse recomputeProgress(String email, int questionsAsked, int maxQuestions, boolean incrementSessions) {
        TrainingProgress progress = trainingProgressRepository.findByEmail(email)
                .orElseGet(() -> TrainingProgress.builder()
                        .email(email)
                        .totalTasks(0)
                        .completedTasks(0)
                        .totalJobAnalyses(0)
                        .totalTrainingSessions(0)
                        .trainingProgressPercent(0)
                        .status(TrainingStatus.NOT_STARTED)
                        .lastActivityAt(null)
                        .lastUpdated(LocalDateTime.now())
                        .lastMatchScore(null)
                        .lastMatchSummary(null)
                        .build()
                );

        LocalDateTime now = LocalDateTime.now();
        long totalTasksCount = trainingTaskRepository.countByEmail(email);
        long completedTasksCount = trainingTaskRepository.countByEmailAndCompletedIsTrue(email);

        int percent;
        if (maxQuestions > 0) {
            percent = (int) Math.round(Math.min(100.0, (completedTasksCount * 100.0) / maxQuestions));
        } else if (totalTasksCount > 0) {
            percent = (int) Math.round((completedTasksCount * 100.0) / totalTasksCount);
        } else {
            percent = 0;
        }

        if (incrementSessions) {
            progress.setTotalTrainingSessions(progress.getTotalTrainingSessions() + 1);
        }

        progress.setTotalTasks((int) totalTasksCount);
        progress.setCompletedTasks((int) completedTasksCount);
        progress.setTrainingProgressPercent(percent);
        if (totalTasksCount > 0 || incrementSessions) {
            progress.setLastActivityAt(now);
        }
        progress.setLastUpdated(now);
        if (totalTasksCount == 0) {
            progress.setStatus(TrainingStatus.NOT_STARTED);
        } else {
            progress.setStatus(percent >= 100 ? TrainingStatus.COMPLETED : TrainingStatus.IN_PROGRESS);
        }

        trainingProgressRepository.save(progress);

        return TrainingProgressResponse.builder()
                .email(email)
                .totalTasks(progress.getTotalTasks())
                .completedTasks(progress.getCompletedTasks())
                .totalJobAnalyses(progress.getTotalJobAnalyses())
                .totalTrainingSessions(progress.getTotalTrainingSessions())
                .trainingProgressPercent(progress.getTrainingProgressPercent())
                .lastActivityAt(progress.getLastActivityAt())
                .status(progress.getStatus())
                .lastMatchScore(progress.getLastMatchScore())
                .lastMatchSummary(progress.getLastMatchSummary())
                .build();
    }

    private void saveTrainingTask(String email,
                                  String skillKey,
                                  String question,
                                  String answer,
                                  String feedback) {
        String taskKey = buildTaskKey(skillKey, question);
        TrainingTask task = trainingTaskRepository.findByEmailAndTaskKey(email, taskKey)
                .orElseGet(TrainingTask::new);

        LocalDateTime now = LocalDateTime.now();
        if (task.getId() == null && task.getCreatedAt() == null) {
            task.setCreatedAt(now);
        }

        task.setEmail(email);
        task.setTaskKey(taskKey);
        task.setSkillKey(skillKey);
        task.setQuestion(question);
        task.setAnswer(answer);
        task.setFeedback(feedback);
        task.setCompleted(true);
        task.setUpdatedAt(now);

        trainingTaskRepository.save(task);
    }

    private String ensureNextQuestion(String skillKey,
                                      String candidate,
                                      int questionsAsked,
                                      int maxQuestions,
                                      String jobSummary,
                                      List<TrainingTask> history,
                                      CoachAnswerRequest request,
                                      String email) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }

        return softSkillSeedService.getNextSeed(skillKey, questionsAsked)
                .map(SoftSkillSeedService.SeedQuestion::getQuestionText)
                .orElseGet(() -> askForNextQuestion(
                        skillKey,
                        appendHistory(history, request),
                        jobSummary,
                        questionsAsked,
                        maxQuestions,
                        email
                ));
    }

    private List<TrainingTask> appendHistory(List<TrainingTask> history, CoachAnswerRequest request) {
        List<TrainingTask> copy = new ArrayList<>(history);
        TrainingTask synthetic = TrainingTask.builder()
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .build();
        copy.add(synthetic);
        return copy;
    }

    private List<TrainingTask> orderedHistory(List<TrainingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<TrainingTask> copy = new ArrayList<>(tasks);
        copy.sort(Comparator.comparing(
                TrainingTask::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
        );
        return copy;
    }

    private String resolveJobSummary(String email) {
        return jobAnalysisSessionRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .map(this::pickSummaryText)
                .orElse(null);
    }

    private String pickSummaryText(JobAnalysisSession session) {
        if (StringUtils.hasText(session.getSummary())) {
            return session.getSummary();
        }
        if (StringUtils.hasText(session.getAiSummary())) {
            return session.getAiSummary();
        }
        if (StringUtils.hasText(session.getAnalysisResult())) {
            return session.getAnalysisResult();
        }
        return null;
    }

    private String resolveCurrentSkill(String email) {
        return trainingTaskRepository.findByEmailOrderByCreatedAtDesc(email).stream()
                .map(TrainingTask::getSkillKey)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("conflict_management");
    }

    private int resolveMaxQuestions(String skillKey) {
        int seeds = softSkillSeedService.getSeedCount(skillKey);
        return seeds > 0 ? seeds : 5;
    }

    private String buildQuestionSystemPrompt(String skillKey) {
        return """
                Sa oled kogenud engineering manager ja soft-skill coach.
                Fookus: %s
                Vastad ainult JSON-iga, et back-end saaks seda otse parsida.
                """.formatted(skillDescription(skillKey));
    }

    private String buildQuestionUserPrompt(String skillKey,
                                           List<TrainingTask> history,
                                           String jobSummary,
                                           int questionsAsked,
                                           int maxQuestions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Treenime teemat: ").append(skillKey).append("\n");
        if (StringUtils.hasText(jobSummary)) {
            sb.append("Tööanalüüsi kokkuvõte (kui aitab fookust seada):\n")
                    .append(jobSummary)
                    .append("\n\n");
        }
        sb.append("Senine treeningu ajalugu:\n");
        if (history.isEmpty()) {
            sb.append("- puudub\n");
        } else {
            int idx = 1;
            for (TrainingTask task : history) {
                if (!StringUtils.hasText(task.getQuestion())) {
                    continue;
                }
                sb.append(idx++).append(") Küsimus: ").append(task.getQuestion()).append("\n");
                if (StringUtils.hasText(task.getAnswer())) {
                    sb.append("   Vastus: ").append(task.getAnswer()).append("\n");
                }
            }
        }

        sb.append("""

                Küsimusi on seni esitatud %d. Maksimaalne lubatud arv on %d.
                Kui maksimaalne on täis, märgi sessionComplete=true ja questionText=null.
                Vasta JSON-iga:
                {
                  "questionText": "...", // järgmine lühike treeningküsimus eesti keeles
                  "sessionComplete": true|false
                }
                """.formatted(questionsAsked, maxQuestions));

        return sb.toString();
    }

    private String buildCoachSystemPrompt(String skillKey) {
        return """
                Sa oled kogenud engineering manager ja käitumuslike oskuste coach.
                Fookussoft-skill: %s.
                Anna tagasisidet lühidalt eesti keeles (1–2 lõiku) ja planeeri loogiline järgmine küsimus.
                Tagasta alati rangelt JSON.
                """.formatted(skillDescription(skillKey));
    }

    private String buildFeedbackPrompt(String skillKey,
                                       List<TrainingTask> history,
                                       String question,
                                       String answer,
                                       String jobSummary,
                                       int askedSoFar,
                                       int maxQuestions) {
        StringBuilder historySection = new StringBuilder();
        if (!history.isEmpty()) {
            historySection.append("Varasemad Q/A paarid:\n");
            int idx = 1;
            for (TrainingTask task : history) {
                if (!StringUtils.hasText(task.getQuestion()) || !StringUtils.hasText(task.getAnswer())) {
                    continue;
                }
                historySection.append(idx++).append(") Q: ")
                        .append(task.getQuestion())
                        .append("\nA: ")
                        .append(task.getAnswer())
                        .append("\n\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Soft-skill võti: ").append(skillKey).append("\n")
                .append("Fookuse kirjeldus: ").append(skillDescription(skillKey)).append("\n")
                .append("Senine vastuste arv: ").append(askedSoFar).append(" / max ").append(maxQuestions).append("\n");

        if (StringUtils.hasText(jobSummary)) {
            sb.append("Job analysis kokkuvõte (kui on):\n")
                    .append(jobSummary)
                    .append("\n\n");
        }

        sb.append(historySection);
        sb.append("Viimane küsimus: ").append(question).append("\n");
        sb.append("Kasutaja vastus: ").append(answer).append("\n\n");

        sb.append("""
                Koosta JSON:
                {
                  "feedback": "1-2 lõiku praktilist tagasisidet eesti keeles",
                  "nextQuestion": "järgmine lühike küsimus samal teemal või null kui sessioon on täis",
                  "sessionComplete": true|false // tee true kui max küsimused täis
                }
                Kui arvud näitavad, et max küsimused on täis, sea sessionComplete=true ja nextQuestion=null.
                """);

        return sb.toString();
    }

    private String skillDescription(String skillKey) {
        return switch (defaultSkillKey(skillKey)) {
            case "technical_initiative" ->
                    "tehniline initsiatiiv, ownership ja lahenduste välja pakkumine";
            case "clear_communication" ->
                    "selge kommunikatsioon, ootuste seadmine ja keeruliste ideede selgitamine";
            case "conflict_management" ->
                    "tiimikonfliktide rahulik ja konstruktiivne lahendamine";
            case "ownership" ->
                    "vastutuse võtmine ja probleemidele omanikuks olemine";
            default ->
                    "üldised koostöö- ja kommunikatsioonioskused";
        };
    }

    private String fallbackQuestion(String skillKey) {
        return switch (defaultSkillKey(skillKey)) {
            case "conflict_management" ->
                    "Kirjelda hiljutist olukorda, kus sinu tiimis tekkis konflikt. Kuidas sa selle lahendasid?";
            case "clear_communication" ->
                    "Räägi olukorrast, kus pidid keerulise tehnilise teema lahti seletama mitte-tehnilisele kolleegile.";
            case "technical_initiative" ->
                    "Kirjelda olukorda, kus võtsid ise initsiatiivi tehnilise probleemi lahendamisel.";
            case "ownership" ->
                    "Räägi olukorrast, kus bug jäi \"õhku rippuma\" ja kuidas sa sellele omanikuks said.";
            default ->
                    "Kirjelda tööolukorda, kus sinu soft-skills mängisid olulist rolli.";
        };
    }

    private String defaultSkillKey(String skillKey) {
        return StringUtils.hasText(skillKey) ? skillKey : "conflict_management";
    }

    private String buildTaskKey(String skillKey, String question) {
        String base = defaultSkillKey(skillKey);
        int hash = StringUtils.hasText(question) ? Math.abs(question.hashCode()) : 0;
        return base + "_softcoach_" + hash;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    private static class CoachModelResult {
        private final String feedback;
        private final String nextQuestion;
        private final boolean sessionComplete;
    }

    public static class OpenAiCoachException extends RuntimeException {
        public OpenAiCoachException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

