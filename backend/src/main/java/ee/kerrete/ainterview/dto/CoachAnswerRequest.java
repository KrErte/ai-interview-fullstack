package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoachAnswerRequest {
    private String skillKey;
    private String question;
    private String answer;
}













