package ee.kerrete.ainterview.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.interview.dto.InterviewIntelligenceResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionRequestDto;
import ee.kerrete.ainterview.model.InterviewSession;
import ee.kerrete.ainterview.repository.InterviewSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic interview flow with persisted questionCount / asked IDs to avoid
 * repeating the opening question.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewIntelligenceService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final ObjectMapper objectMapper;
    private ObjectMapper lenientMapper;

    private QuestionBank bank;

    @jakarta.annotation.PostConstruct
    void initMapper() {
        lenientMapper = objectMapper.copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Transactional
    public InterviewIntelligenceResponseDto nextQuestion(UUID sessionUuid, InterviewNextQuestionRequestDto request) {
        ensureMapper();
        if (sessionUuid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session UUID format");
        }
        InterviewSession session = interviewSessionRepository.findBySessionUuid(sessionUuid)
            .orElseThrow(() -> new EntityNotFoundException("Interview session not found: " + sessionUuid));

        String trimmedAnswer = request == null ? null : request.answer();
        trimmedAnswer = trimmedAnswer == null ? null : trimmedAnswer.trim();

        List<QaEntry> qa = readQa(session.getQuestionAnswers());
        List<Double> scores = readScores(session.getQuestionAnswers());

        // Persist answer to the last served question (if any)
        if (trimmedAnswer != null && !trimmedAnswer.isBlank() && !qa.isEmpty()) {
            QaEntry last = qa.get(qa.size() - 1);
            if (last.getAnswer() == null) {
                last.setAnswer(trimmedAnswer);
            }
            updateAnswerBuffers(session, trimmedAnswer);
            scores.add(score(trimmedAnswer));
        }

        int questionCount = Optional.ofNullable(session.getQuestionCount()).orElse(qa.size());
        questionCount = Math.max(questionCount, qa.size());
        int answered = scores.size();

        String decision = questionCount == 0 ? "opening" : "probe";

        Set<String> askedIds = new LinkedHashSet<>();
        for (QaEntry e : qa) {
            if (e.getQuestionId() != null) {
                askedIds.add(e.getQuestionId());
            }
        }

        Question pick = selectQuestion(decision, askedIds);
        if (pick == null) {
            double last1 = averageLast(scores, 1);
            double last3 = averageLast(scores, 3);
            double last5 = averageLast(scores, 5);
            FitBlock fit = computeFit(answered, last1, last3, last5);
            InterviewIntelligenceResponseDto.FitBreakdown breakdown = buildBreakdown(answered, fit, last3, last5);
            return InterviewIntelligenceResponseDto.builder()
                .question(null)
                .decision("complete")
                .fitScore(fit.fitScore)
                .fitTrend(fit.fitTrend)
                .progress(progress(questionCount, last1, last3, last5))
                .fit(fit.toDto())
                .fitBreakdown(breakdown)
                .sessionComplete(true)
                .build();
        }

        // Mark served: add QA entry, increment questionCount, persist
        qa.add(new QaEntry(pick.id(), pick.text(), null));
        questionCount += 1;
        session.setQuestionCount(questionCount);
        session.setQuestionAnswers(writeQa(qa));
        session.setCreatedAt(Optional.ofNullable(session.getCreatedAt()).orElse(LocalDateTime.now()));

        interviewSessionRepository.save(session);

        double last1 = averageLast(scores, 1);
        double last3 = averageLast(scores, 3);
        double last5 = averageLast(scores, 5);
        FitBlock fit = computeFit(answered, last1, last3, last5);
        InterviewIntelligenceResponseDto.FitBreakdown breakdown = buildBreakdown(answered, fit, last3, last5);

        return InterviewIntelligenceResponseDto.builder()
            .question(pick.text())
            .decision(decision)
            .fitScore(fit.fitScore)
            .fitTrend(fit.fitTrend)
            .progress(progress(questionCount, last1, last3, last5))
            .fit(fit.toDto())
            .fitBreakdown(breakdown)
            .sessionComplete(false)
            .build();
    }

    private InterviewIntelligenceResponseDto.Progress progress(int qCount, double last1, double last3, double last5) {
        return InterviewIntelligenceResponseDto.Progress.builder()
            .questionCount(qCount)
            .currentDimension(bank().dimensions().isEmpty() ? null : bank().dimensions().get(0).key())
            .last1Average(last1)
            .last3Average(last3)
            .last5Average(last5)
            .build();
    }

    private void updateAnswerBuffers(InterviewSession session, String answer) {
        session.setLast1Answer(answer);

        List<String> last3 = readStringList(session.getLast3Answers());
        appendCapped(last3, answer, 3);
        session.setLast3Answers(writeStringList(last3));

        List<String> last5 = readStringList(session.getLast5Answers());
        appendCapped(last5, answer, 5);
        session.setLast5Answers(writeStringList(last5));
    }

    private void appendCapped(List<String> list, String value, int cap) {
        list.add(value);
        while (list.size() > cap) {
            list.remove(0);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return lenientMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (IOException e) {
            return "[]";
        }
    }

    private List<QaEntry> readQa(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return lenientMapper.readValue(json, new TypeReference<>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<Double> readScores(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> raw = lenientMapper.readValue(json, new TypeReference<>() {});
            List<Double> scores = new ArrayList<>();
            for (Map<String, Object> m : raw) {
                if (m == null) continue;
                if (m.containsKey("avg")) {
                    Object v = m.get("avg");
                    if (v instanceof Number n) {
                        scores.add(n.doubleValue());
                        continue;
                    }
                }
                Object ans = m.get("answer");
                if (ans != null) {
                    scores.add(score(String.valueOf(ans)));
                }
            }
            return scores;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private String writeQa(List<QaEntry> qa) {
        try {
            return objectMapper.writeValueAsString(qa == null ? List.of() : qa);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize questionAnswers", e);
        }
    }

    private Question selectQuestion(String decision, Set<String> askedIds) {
        List<Dimension> dimensions = bank().dimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            return null;
        }

        List<String> types = List.of(decision);
        if (!decision.equals("challenge")) {
            types = List.of(decision, "probe", "challenge", "opening");
        }

        for (String type : types) {
            for (Dimension d : dimensions) {
                List<String> pool = poolForType(d.questions(), type);
                for (int i = 0; i < pool.size(); i++) {
                    String qText = pool.get(i);
                    String qId = d.key() + ":" + type + ":" + i;
                    if (!askedIds.contains(qId)) {
                        return new Question(qId, qText);
                    }
                }
            }
        }
        return null;
    }

    private List<String> poolForType(Questions q, String type) {
        return switch (type) {
            case "opening" -> q.opening();
            case "challenge" -> q.challenge();
            default -> q.probe();
        };
    }

    private synchronized QuestionBank bank() {
        ensureMapper();
        if (bank == null) {
            bank = loadBank();
        }
        return bank;
    }

    private QuestionBank loadBank() {
        ensureMapper();
        try (var is = getClass().getClassLoader().getResourceAsStream("spec/question-bank-seed-v1.json")) {
            if (is == null) {
                throw new IllegalStateException("question-bank-seed-v1.json not found");
            }
            return lenientMapper.readValue(is, QuestionBank.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load question bank", e);
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void ensureMapper() {
        if (lenientMapper == null) {
            lenientMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }

    private double score(String answer) {
        if (answer == null || answer.isBlank()) return 0.0;
        int words = answer.trim().split("\\s+").length;
        double raw = (words / 20.0) * 5.0;
        return clamp(raw, 0.0, 5.0);
    }

    private record Question(String id, String text) {}

    private FitBlock computeFit(int answered, double last1, double last3, double last5) {
        if (answered < 3) {
            return FitBlock.notComputed();
        }

        int fitScore = (int) Math.round(clamp(last3 / 5.0 * 100.0, 0.0, 100.0));
        double delta = answered >= 5 ? last3 - last5 : last1 - last3;
        String trend;
        if (delta >= 0.5) {
            trend = "improving";
        } else if (delta <= -0.5) {
            trend = "declining";
        } else {
            trend = "flat";
        }

        return new FitBlock(true, fitScore, trend, (double) fitScore);
    }

    private InterviewIntelligenceResponseDto.FitBreakdown buildBreakdown(int answered, FitBlock fit, double last3, double last5) {
        if (answered < 3 || !fit.computed) {
            return InterviewIntelligenceResponseDto.FitBreakdown.builder()
                .confidence("LOW")
                .answeredCount(answered)
                .dimensions(List.of())
                .build();
        }

        String confidence;
        double variance = Math.abs(last3 - last5);
        if (answered >= 8 && variance < 0.3) {
            confidence = "HIGH";
        } else if (variance < 0.7) {
            confidence = "MEDIUM";
        } else {
            confidence = "LOW";
        }

        Dimension primary = bank().dimensions().isEmpty() ? null : bank().dimensions().get(0);
        String key = primary == null ? "general" : primary.key();
        String label = primary == null ? "General" : primary.name();

        int scorePercent = fit.fitScore == null ? 0 : fit.fitScore;
        String band = scorePercent >= 80 ? "STRONG" : scorePercent >= 60 ? "GOOD" : "NEEDS_WORK";

        String trendEnum = switch (fit.fitTrend == null ? "STABLE" : fit.fitTrend.toLowerCase()) {
            case "improving" -> "IMPROVING";
            case "declining" -> "DECLINING";
            default -> "STABLE";
        };

        List<InterviewIntelligenceResponseDto.Insight> insights = selectInsights(key, band, trendEnum);

        InterviewIntelligenceResponseDto.DimensionBreakdown dim = InterviewIntelligenceResponseDto.DimensionBreakdown.builder()
            .key(key)
            .label(label)
            .scorePercent(scorePercent)
            .band(band)
            .insights(insights)
            .build();

        return InterviewIntelligenceResponseDto.FitBreakdown.builder()
            .confidence(confidence)
            .answeredCount(answered)
            .dimensions(List.of(dim))
            .build();
    }

    private List<InterviewIntelligenceResponseDto.Insight> selectInsights(String key, String band, String trend) {
        List<String> templates = InsightTemplates.templates()
            .getOrDefault(key, InsightTemplates.templates().getOrDefault("generic", Map.of()))
            .getOrDefault(band, Map.of())
            .getOrDefault(trend, InsightTemplates.genericList());

        List<InterviewIntelligenceResponseDto.Insight> list = new ArrayList<>();
        for (int i = 0; i < Math.min(3, templates.size()); i++) {
            String txt = templates.get(i);
            String type = txt.toLowerCase().contains("risk") || txt.toLowerCase().contains("improve") ? "RISK" : "STRENGTH";
            list.add(InterviewIntelligenceResponseDto.Insight.builder().type(type).text(txt).build());
        }
        if (list.size() < 2) {
            list.add(InterviewIntelligenceResponseDto.Insight.builder().type("STRENGTH").text("Consistent performance noted.").build());
            list.add(InterviewIntelligenceResponseDto.Insight.builder().type("RISK").text("Look for deeper examples to validate.").build());
        }
        return list;
    }

    private static class InsightTemplates {
        private static Map<String, Map<String, Map<String, List<String>>>> cache;

        static Map<String, Map<String, Map<String, List<String>>>> templates() {
            if (cache != null) return cache;
            Map<String, Map<String, List<String>>> genericBands = Map.of(
                "STRONG", Map.of(
                    "IMPROVING", List.of("Momentum is improving; keep sharing concrete wins.", "Strength evident; continue to deepen impact."),
                    "STABLE", List.of("Consistent strength; maintain clarity and depth.", "Solid performance; consider stretching into harder examples."),
                    "DECLINING", List.of("Strength areas present; ensure recent examples stay sharp.", "Guard against complacency; refresh narratives.")
                ),
                "GOOD", Map.of(
                    "IMPROVING", List.of("Trajectory is positive; highlight recent progress.", "Growing strength; add measurable outcomes."),
                    "STABLE", List.of("Good baseline; add specifics to solidify impact.", "Solid answers; deepen with metrics and trade-offs."),
                    "DECLINING", List.of("Stability slipping; focus on recent wins.", "Address gaps with clearer ownership and results.")
                ),
                "NEEDS_WORK", Map.of(
                    "IMPROVING", List.of("Early improvements; keep adding concrete detail.", "Progressing; prepare sharper examples."),
                    "STABLE", List.of("Needs clearer ownership and outcomes.", "Provide structured narratives with decisions and results."),
                    "DECLINING", List.of("Recent answers regress; tighten structure and evidence.", "Rebuild with specific actions and results.")
                )
            );
            cache = Map.of(
                "generic", genericBands
            );
            return cache;
        }

        static List<String> genericList() {
            return List.of("Provide concrete examples with outcomes.", "Clarify your personal contribution.");
        }
    }

    private double averageLast(List<Double> scores, int n) {
        if (scores == null || scores.isEmpty()) return 0.0;
        int from = Math.max(0, scores.size() - n);
        List<Double> sub = scores.subList(from, scores.size());
        return sub.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class QaEntry {
        private String questionId;
        private String question;
        private String answer;

        public QaEntry() {
        }

        public QaEntry(String questionId, String question, String answer) {
            this.questionId = questionId;
            this.question = question;
            this.answer = answer;
        }

        public String getQuestionId() {
            return questionId;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    private record QuestionBank(String version, Constraints constraints, List<String> styles, List<Dimension> dimensions) { }

    private record Constraints(boolean deterministic, boolean no_llm_calls, String selection_strategy) { }

    private record Dimension(String key, String name, Questions questions) { }

    private record Questions(List<String> opening, List<String> probe, List<String> challenge) { }

    private record FitBlock(boolean computed, Integer fitScore, String fitTrend, Double overall) {
        static FitBlock notComputed() {
            return new FitBlock(false, null, null, null);
        }

        InterviewIntelligenceResponseDto.Fit toDto() {
            return InterviewIntelligenceResponseDto.Fit.builder()
                .computed(computed)
                .overall(overall)
                .currentDimension(null)
                .trend(null)
                .build();
        }
    }
}
