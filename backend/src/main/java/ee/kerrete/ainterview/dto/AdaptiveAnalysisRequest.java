package ee.kerrete.ainterview.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptiveAnalysisRequest {
    private String email;
    private String roadmapItemId;
    private String question;
    private String answer;
    private Map<String, Integer> currentSkillSnapshot; // optional
}
