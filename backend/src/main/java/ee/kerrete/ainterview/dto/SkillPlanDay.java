package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ühe päeva treeninguplaani info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillPlanDay {

    private int dayNumber;

    private String title;

    private String description;

    private String practiceTask;
}
