package ee.kerrete.ainterview.softskills.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SoftSkillMergedProfileView {

    String email;
    List<SoftSkillMergedDimensionView> dimensions;
}


