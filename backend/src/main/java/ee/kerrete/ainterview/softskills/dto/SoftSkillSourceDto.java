package ee.kerrete.ainterview.softskills.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillSourceDto {

    /**
     * Source type, e.g. HR, TECH, TEAM_LEAD, SELF, OTHER.
     */
    @NotBlank
    private String sourceType;

    /**
     * Optional human-friendly label for the source.
     */
    private String label;

    /**
     * Raw feedback text.
     */
    @NotBlank
    private String content;
}

