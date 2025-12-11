package ee.kerrete.ainterview.interview.service;

import ee.kerrete.ainterview.interview.dto.InterviewDimensionScoreDto;
import ee.kerrete.ainterview.interview.dto.InterviewNextQuestionResponseDto;
import ee.kerrete.ainterview.interview.dto.InterviewSummaryResponseDto;
import ee.kerrete.ainterview.interview.model.InterviewAnswer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnswerAnalysisService {

    public InterviewNextQuestionResponseDto.LocalAnalysisDto localAnalysis(List<InterviewAnswer> answers) {
        List<String> normalized = answers.stream()
            .map(InterviewAnswer::getAnswer)
            .filter(StringUtils::hasText)
            .map(a -> a.toLowerCase(Locale.ROOT))
            .toList();

        List<String> last3 = lastN(normalized, 3);
        Map<String, Long> tokenFreq = tokenize(last3);

        List<String> strengths = tokenFreq.entrySet().stream()
            .filter(e -> e.getKey().matches("clear|team|impact|design|metrics|learn"))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> "Shows " + e.getKey())
            .limit(3)
            .toList();

        List<String> risks = tokenFreq.entrySet().stream()
            .filter(e -> e.getKey().matches("maybe|unsure|problem|issue|delay|uncertain"))
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(e -> "Potential concern: " + e.getKey())
            .limit(3)
            .toList();

        return InterviewNextQuestionResponseDto.LocalAnalysisDto.builder()
            .detectedStrengths(strengths)
            .detectedRisks(risks)
            .build();
    }

    public InterviewSummaryResponseDto buildSummary(List<InterviewAnswer> answers) {
        List<String> cleaned = answers.stream()
            .map(InterviewAnswer::getAnswer)
            .filter(StringUtils::hasText)
            .map(a -> a.trim().toLowerCase(Locale.ROOT))
            .toList();

        List<InterviewDimensionScoreDto> dimensionScores = dimensionScores(cleaned);
        List<String> strengths = topDimensions(dimensionScores, true);
        List<String> weaknesses = topDimensions(dimensionScores, false);

        double fitScore = dimensionScores.stream()
            .mapToDouble(InterviewDimensionScoreDto::getScore)
            .average()
            .orElse(0.5);

        String verdict = fitScore >= 0.75 ? "Strong fit"
            : fitScore >= 0.55 ? "Moderate fit – some gaps"
            : "Risky fit – needs improvement";

        return InterviewSummaryResponseDto.builder()
            .strengths(strengths)
            .weaknesses(weaknesses)
            .dimensionScores(dimensionScores)
            .verdict(verdict)
            .build();
    }

    private List<InterviewDimensionScoreDto> dimensionScores(List<String> answers) {
        Map<String, Long> tokenFreq = tokenize(answers);

        return List.of(
            dimensionScore("ownership", averageScore(tokenFreq, List.of("own", "drive", "respons", "lead"))),
            dimensionScore("communication", averageScore(tokenFreq, List.of("communicat", "clear", "present", "explain"))),
            dimensionScore("collaboration", averageScore(tokenFreq, List.of("team", "together", "collabor", "stakeholder"))),
            dimensionScore("problem_solving", averageScore(tokenFreq, List.of("solve", "debug", "design", "issue", "trade-off"))),
            dimensionScore("learning_agility", averageScore(tokenFreq, List.of("learn", "adapt", "feedback", "improve"))),
            dimensionScore("leadership", averageScore(tokenFreq, List.of("lead", "mentor", "coach", "align"))),
            dimensionScore("resilience", averageScore(tokenFreq, List.of("stress", "pressure", "resilien", "recover", "fail"))),
            dimensionScore("technical_communication", averageScore(tokenFreq, List.of("api", "design", "diagram", "architecture", "trade-off")))
        );
    }

    private List<String> topDimensions(List<InterviewDimensionScoreDto> scores, boolean strengths) {
        if (scores == null || scores.isEmpty()) return List.of();
        return scores.stream()
            .sorted((a, b) -> strengths
                ? Double.compare(b.getScore(), a.getScore())
                : Double.compare(a.getScore(), b.getScore()))
            .limit(3)
            .map(s -> strengths ? "Strong in " + s.getDimension() : "Needs work in " + s.getDimension())
            .toList();
    }

    private List<String> lastN(List<String> answers, int n) {
        if (answers == null || answers.isEmpty()) {
            return List.of();
        }
        return answers.stream()
            .skip(Math.max(0, answers.size() - n))
            .toList();
    }

    private Map<String, Long> tokenize(List<String> slice) {
        return slice.stream()
            .flatMap(a -> List.of(a.toLowerCase(Locale.ROOT).split("\\W+")).stream())
            .filter(StringUtils::hasText)
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }

    private InterviewDimensionScoreDto dimensionScore(String dimension, double score) {
        double bounded = Math.max(0.0, Math.min(1.0, score));
        return InterviewDimensionScoreDto.builder()
            .dimension(dimension)
            .score(bounded)
            .build();
    }

    private double averageScore(Map<String, Long> tokens, List<String> keywords) {
        if (tokens.isEmpty()) {
            return 0.5;
        }
        long hits = keywords.stream()
            .mapToLong(k -> tokens.getOrDefault(k, 0L))
            .sum();
        double ratio = (double) hits / Math.max(5, tokens.values().stream().mapToLong(Long::longValue).sum());
        return Math.min(1.0, 0.3 + ratio * 1.5);
    }
}

