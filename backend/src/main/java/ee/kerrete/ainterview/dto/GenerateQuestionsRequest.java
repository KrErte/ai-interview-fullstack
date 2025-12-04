package ee.kerrete.ainterview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsRequest {

    private String email;
    private String cvText;
    private int technicalCount = 8;
    private int softCount = 4;
}
