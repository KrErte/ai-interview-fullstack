package ee.kerrete.ainterview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvQuestionsRequest {

    @NotBlank
    private String cvText;

    @Min(0)
    private int techCount;

    @Min(0)
    private int softCount;
}
