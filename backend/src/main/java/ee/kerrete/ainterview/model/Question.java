package ee.kerrete.ainterview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)   // <-- LISA SEE
public class Question {

    private String id;
    private String question;
    private String difficulty;
    private String category;

}
