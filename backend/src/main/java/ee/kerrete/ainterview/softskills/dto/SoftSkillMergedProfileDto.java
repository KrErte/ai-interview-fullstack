package ee.kerrete.ainterview.softskills.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Value
@Builder
@With
@JsonIgnoreProperties(ignoreUnknown = true)
public class SoftSkillMergedProfileDto {

    @Builder.Default
    String summary = "No summary provided.";
    @Builder.Default
    List<String> strengths = List.of();
    @Builder.Default
    List<String> risks = List.of();
    @Builder.Default
    String communicationStyle = "Not provided";
    @Builder.Default
    String collaborationStyle = "Not provided";
    @Builder.Default
    List<String> growthAreas = List.of();
    @Builder.Default
    List<MergedDimensionScore> dimensionScoresMerged = List.of();
    Meta meta;
}

