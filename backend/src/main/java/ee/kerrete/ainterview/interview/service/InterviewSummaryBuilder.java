package ee.kerrete.ainterview.interview.service;

import ee.kerrete.ainterview.interview.dto.InterviewDimensionScoreDto;
import ee.kerrete.ainterview.interview.dto.InterviewSummaryResponseDto;
import ee.kerrete.ainterview.interview.model.InterviewSessionState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewSummaryBuilder {

    private static final List<String> MAIN_DIMENSIONS = List.of(
        "ownership",
        "communication",
        "collaboration",
        "problem_solving",
        "learning_agility",
        "leadership"
    );

    private final AnswerAnalysisService analysisService;

    public InterviewSummaryBuilder(AnswerAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public InterviewSummaryResponseDto buildSummaryResponse(InterviewSessionState state) {
        var summary = analysisService.buildSummary(state.getAnswers());
        double fit = averageMain(summary.getDimensionScores());
        return InterviewSummaryResponseDto.builder()
            .sessionId(state.getSessionId())
            .finished(true)
            .fitScore(fit)
            .strengths(summary.getStrengths())
            .weaknesses(summary.getWeaknesses())
            .dimensionScores(summary.getDimensionScores())
            .verdict(summary.getVerdict())
            .build();
    }

    private double averageMain(List<InterviewDimensionScoreDto> scores) {
        if (scores == null || scores.isEmpty()) return 0.5;
        return scores.stream()
            .filter(s -> MAIN_DIMENSIONS.contains(s.getDimension()))
            .mapToDouble(InterviewDimensionScoreDto::getScore)
            .average()
            .orElse(0.5);
    }
}

