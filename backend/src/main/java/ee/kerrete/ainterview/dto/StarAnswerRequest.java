package ee.kerrete.ainterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StarAnswerRequest {
    private String question;
    private String cvText;
    private String jobDescription;
}
