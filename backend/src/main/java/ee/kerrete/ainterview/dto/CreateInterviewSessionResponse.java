package ee.kerrete.ainterview.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionResponse {

    private Long sessionId;
    private String email;
    private java.util.UUID sessionUuid;
}
