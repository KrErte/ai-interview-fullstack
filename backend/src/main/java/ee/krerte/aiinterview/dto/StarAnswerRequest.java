package ee.krerte.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StarAnswerRequest {
    private String question;
    private String cvText;
    private String jobDescription;
}
