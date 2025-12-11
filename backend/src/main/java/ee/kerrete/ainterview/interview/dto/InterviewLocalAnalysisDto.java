package ee.kerrete.ainterview.interview.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class InterviewLocalAnalysisDto {
    List<String> detectedStrengths;
    List<String> detectedRisks;
    Integer alignmentScore; // 0-100 simple heuristic
}

