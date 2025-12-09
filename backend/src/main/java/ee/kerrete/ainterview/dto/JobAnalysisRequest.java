package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobAnalysisRequest {

    private String email;          // võib olla null

    @NotBlank
    private String cvText;        // CV tekst (kas copy-paste või PDF-ist)

    @NotBlank
    private String jobDescription;
}
