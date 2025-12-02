package ee.krerte.aiinterview.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobAnalysisRequest {

    private String email;          // võib olla null
    private String cvText;        // CV tekst (kas copy-paste või PDF-ist)
    private String jobDescription;
}
