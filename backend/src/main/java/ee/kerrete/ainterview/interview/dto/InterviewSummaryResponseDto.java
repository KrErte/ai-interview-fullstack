package ee.kerrete.ainterview.interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class InterviewSummaryResponseDto {
    UUID sessionId;
    @JsonProperty("isFinished")
    @Builder.Default
    boolean finished = true;
    double fitScore;
    List<String> strengths;
    List<String> weaknesses;
    List<InterviewDimensionScoreDto> dimensionScores;
    String verdict;
}

