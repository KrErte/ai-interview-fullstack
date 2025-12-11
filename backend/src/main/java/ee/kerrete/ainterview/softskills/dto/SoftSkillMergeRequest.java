package ee.kerrete.ainterview.softskills.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoftSkillMergeRequest {

    /**
     * TODO: Derive email from authenticated principal once security is wired.
     */
    @NotBlank
    @Email
    private String email;

    @Valid
    @NotEmpty
    @Builder.Default
    private List<SoftSkillSourceDto> sources = new ArrayList<>();

    /**
     * Whether to persist the merged profile. Defaults to false.
     */
    @Builder.Default
    private Boolean saveMerged = false;
}

