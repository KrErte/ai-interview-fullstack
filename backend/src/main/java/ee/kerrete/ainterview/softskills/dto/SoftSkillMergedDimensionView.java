package ee.kerrete.ainterview.softskills.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SoftSkillMergedDimensionView {

    String dimensionKey;
    double averageScore;
    int ratingCount;
}


