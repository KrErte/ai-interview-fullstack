package ee.kerrete.ainterview.softskills.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoftSkillDimensionDto {

    private String key;
    private String displayName;
    private String description;
}

