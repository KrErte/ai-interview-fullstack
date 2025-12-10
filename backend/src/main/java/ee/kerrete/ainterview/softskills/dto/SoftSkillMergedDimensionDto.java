package ee.kerrete.ainterview.softskills.dto;

import ee.kerrete.ainterview.softskills.enums.SoftSkillDimension;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SoftSkillMergedDimensionDto {

    SoftSkillDimension dimension;
    Integer mergedScore;
    String explanation;
}


