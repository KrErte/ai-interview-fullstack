package ee.kerrete.ainterview.softskills.dto;

import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import ee.kerrete.ainterview.softskills.enums.SoftSkillSource;
import lombok.Data;

/**
 * Incoming request for storing a single soft-skill evaluation.
 */
@Data
public class SoftSkillEvaluationRequest {

    /**
     * Candidate email this evaluation belongs to.
     */
    private String email;

    private SoftSkillDimension dimension;

    private SoftSkillSource source;

    /**
     * Score in range [0, 100].
     */
    private Integer score;

    /**
     * Free-text comment from the evaluator.
     */
    private String comment;
}


