package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SaveLearningPlanRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String jobTitle;

    @NotEmpty
    private List<String> missingSkills;

    @NotEmpty
    private List<String> roadmap;
}
