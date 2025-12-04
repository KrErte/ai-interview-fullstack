package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Soft-skillide “radar” profiil, mida front saab graafikuna kuvada.
 *
 * Skaala: 0.0 – 5.0 (kus 5.0 = väga tugev).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftSkillMatrixResponse {

    private String email;

    private double starStructure;
    private double conflictResolution;
    private double emotionalIntelligence;
    private double leadership;
    private double reflection;
    private double psychologicalSafety;

    /**
     * Üldine koondskoor (0–100).
     */
    private double overallScore;

    /**
     * Sõnaline tase: JUNIOR, MID, SENIOR_READY, LEAD_POTENTIAL, NO_DATA.
     */
    private String overallLevel;

    /**
     * Mitu vastust analüüsi aluseks kasutati.
     */
    private int totalAnswers;

    /**
     * Viimase kasutatud vastuse aeg (ISO stringina) või null.
     */
    private String lastUpdated;

    public static SoftSkillMatrixResponse empty(String email) {
        return SoftSkillMatrixResponse.builder()
                .email(email)
                .starStructure(0.0)
                .conflictResolution(0.0)
                .emotionalIntelligence(0.0)
                .leadership(0.0)
                .reflection(0.0)
                .psychologicalSafety(0.0)
                .overallScore(0.0)
                .overallLevel("NO_DATA")
                .totalAnswers(0)
                .lastUpdated(null)
                .build();
    }
}
