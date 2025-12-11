package ee.kerrete.ainterview.softskills.catalog.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SoftSkillDimensionResponseDto {
    String key;
    String label;
    String definition;
    List<String> highSignals;
    List<String> lowSignals;
    List<String> interviewSignals;
    List<String> coachingIdeas;
}

