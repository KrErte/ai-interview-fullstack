package ee.kerrete.ainterview.softskills.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillEvaluationRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String sourceType;

    @Valid
    @NotEmpty
    private List<SoftSkillScoreRequest> scores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SoftSkillScoreRequest {
        @NotBlank
        private String dimensionKey;

        @NotNull
        private Integer score;

        private String explanation;
    }
}
