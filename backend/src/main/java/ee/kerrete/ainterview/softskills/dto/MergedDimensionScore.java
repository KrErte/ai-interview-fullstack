package ee.kerrete.ainterview.softskills.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergedDimensionScore {
    String dimension;
    Double mergedScore;
    Double confidence;
    String rationale;
}

