package ee.kerrete.ainterview.softskills.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Meta {
    Double overallConfidence;
    @Builder.Default
    List<String> mainDisagreements = List.of();
    @Builder.Default
    List<String> notesForCoach = List.of();
}

