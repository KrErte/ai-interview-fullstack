package ee.kerrete.ainterview.interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class InterviewStartResponse {
    UUID sessionId;
    int questionNumber;
    int totalQuestions;
    String question;
    String modelAnswerHint;
    @JsonProperty("isFinished")
    @Builder.Default
    boolean finished = false;
}

