package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Vastus, mida front kasutab 21-päevase plaani kuvamiseks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillPlanResponse {

    /**
     * Üldine eesmärk, mida see plaan aitab saavutada.
     */
    private String overallGoal;

    /**
     * Päevade kaupa jaotatud treeningplaan.
     */
    private List<SkillPlanDay> days;

    /**
     * true when this is the local template plan (no OpenAI key or the AI call failed) instead of an AI plan
     * (FP-20, same flag as JobAnalysisResponse).
     */
    @Builder.Default
    private boolean fallback = false;
}
