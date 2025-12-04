package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LearningPlanDto {

    private String email;
    private String jobTitle;
    private List<String> missingSkills;
    private List<String> roadmap;
}
