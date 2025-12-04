package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptiveAnalysisResponse {

    private String feedback;
    private String nextQuestion;
    private String weakestSkill;

    @Builder.Default
    private Map<String, Integer> updatedSkills = Collections.emptyMap();

    private String error;

    public static AdaptiveAnalysisResponse error(String message) {
        return AdaptiveAnalysisResponse.builder()
                .error(message)
                .build();
    }
}
