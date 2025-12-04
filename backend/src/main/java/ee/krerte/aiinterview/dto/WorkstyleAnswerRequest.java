package ee.krerte.aiinterview.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class WorkstyleAnswerRequest {
    private UUID sessionId;
    private String answer;
}
