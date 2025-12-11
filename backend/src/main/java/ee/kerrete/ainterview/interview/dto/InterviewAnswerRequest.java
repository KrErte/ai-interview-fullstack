package ee.kerrete.ainterview.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class InterviewAnswerRequest {
    @NotNull
    UUID sessionId;
    @NotNull
    Integer questionNumber;
    @NotBlank
    String answer;
}

