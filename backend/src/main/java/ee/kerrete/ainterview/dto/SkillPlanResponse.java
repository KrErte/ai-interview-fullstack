package ee.kerrete.ainterview.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Vastus, mida front kasutab 21-päevase plaani kuvamiseks.
 */
@Data
@Builder
public class SkillPlanResponse {

    /**
     * Üldine eesmärk, mida see plaan aitab saavutada.
     */
    private String overallGoal;

    /**
     * Päevade kaupa jaotatud treeningplaan.
     */
    private List<SkillPlanDay> days;
}
