package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterviewDimensionScoreDto {
    String dimension;
    double score;
}
