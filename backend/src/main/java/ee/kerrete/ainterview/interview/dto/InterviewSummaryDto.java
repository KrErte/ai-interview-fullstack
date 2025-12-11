package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InterviewSummaryDto {
    @Builder.Default
    boolean finished = true;
    double fitScore;
    List<String> strengths;
    List<String> weaknesses;
    List<InterviewDimensionScoreDto> dimensionScores;
    String verdict;
}

