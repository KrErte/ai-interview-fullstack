package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ühe roadmapi sammu kirjeldus profiili vaates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapStepDto {

    private String key;
    private String title;
    private String description;

    /**
     * Kas samm on juba lõpetatud.
     */
    private boolean completed;

    /**
     * Sammule omane progress (0–100).
     */
    private int progressPercent;

    /**
     * Kas tegemist on praeguse "fookus" sammuga.
     */
    private boolean current;
}
