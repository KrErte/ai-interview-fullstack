package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ühe skill'i koondinfo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDimensionDto {

    /**
     * Tehniline võti, nt "initiative", "teamwork".
     */
    private String key;

    /**
     * Kuvanimi UI jaoks, nt "Initsiatiiv", "Tiimitöö".
     */
    private String label;

    /**
     * Mitu ülesannet / küsimust selle skilliga on andmebaasis.
     */
    private int totalTasks;

    /**
     * Mitu neist on kasutaja juba vastanud / lõpetanud.
     */
    private int completedTasks;

    /**
     * Valmisoleku protsent 0–100.
     */
    private int completionPercent;
}
